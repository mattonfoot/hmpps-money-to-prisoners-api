package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRuleState
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AutoAcceptRuleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SecurityCheckRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import java.time.LocalDateTime

class SecurityCheckConflictException(message: String) : RuntimeException(message)

@Service
class SecurityCheckService(
  private val securityCheckRepository: SecurityCheckRepository,
  private val senderProfileRepository: SenderProfileRepository,
  private val prisonerProfileRepository: PrisonerProfileRepository,
  private val autoAcceptRuleRepository: AutoAcceptRuleRepository,
) {

  @Transactional
  fun acceptCheck(id: Long, username: String, decisionReason: String) {
    val check = securityCheckRepository.findById(id)
      .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SecurityCheck $id not found") }

    when (check.status) {
      CheckStatus.ACCEPTED -> return // idempotent
      CheckStatus.REJECTED -> throw SecurityCheckConflictException("Cannot accept a check that is already rejected")
      CheckStatus.PENDING -> {
        check.status = CheckStatus.ACCEPTED
        check.decisionReason = decisionReason
        check.actionedBy = username
        check.actionedAt = LocalDateTime.now()
        securityCheckRepository.save(check)
      }
    }
  }

  @Transactional
  fun rejectCheck(id: Long, username: String, decisionReason: String, rejectionReasons: List<String>) {
    val check = securityCheckRepository.findById(id)
      .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SecurityCheck $id not found") }

    when (check.status) {
      CheckStatus.REJECTED -> return // idempotent
      CheckStatus.ACCEPTED -> throw SecurityCheckConflictException("Cannot reject a check that is already accepted")
      CheckStatus.PENDING -> {
        check.status = CheckStatus.REJECTED
        check.decisionReason = decisionReason
        check.rejectionReasons = rejectionReasons.joinToString(",")
        check.actionedBy = username
        check.actionedAt = LocalDateTime.now()
        securityCheckRepository.save(check)
      }
    }
  }

  fun listChecks(
    status: CheckStatus? = null,
    rules: String? = null,
    startedAtGte: LocalDateTime? = null,
    startedAtLt: LocalDateTime? = null,
  ): List<SecurityCheck> {
    val spec = buildSpecification(status, rules, startedAtGte, startedAtLt)
    return securityCheckRepository.findAll(spec)
  }

  private fun buildSpecification(
    status: CheckStatus?,
    rules: String?,
    startedAtGte: LocalDateTime?,
    startedAtLt: LocalDateTime?,
  ): Specification<SecurityCheck> {
    val specs = mutableListOf<Specification<SecurityCheck>>()

    if (status != null) {
      specs.add(Specification { root, _, cb -> cb.equal(root.get<CheckStatus>("status"), status) })
    }

    if (rules != null) {
      specs.add(
        Specification { root, _, cb ->
          cb.like(cb.lower(root.get("ruleCodes")), "%${rules.lowercase()}%")
        },
      )
    }

    if (startedAtGte != null) {
      specs.add(
        Specification { root, _, cb ->
          cb.greaterThanOrEqualTo(root.get("startedAt"), startedAtGte)
        },
      )
    }

    if (startedAtLt != null) {
      specs.add(
        Specification { root, _, cb ->
          cb.lessThan(root.get("startedAt"), startedAtLt)
        },
      )
    }

    if (specs.isEmpty()) return Specification { _, _, cb -> cb.conjunction() }
    return specs.reduce { acc, s -> acc.and(s) }
  }

  @Transactional
  fun createAutoAcceptRule(
    senderProfileId: Long,
    prisonerProfileId: Long,
    initialState: AutoAcceptRuleState.() -> Unit,
    createdBy: String?,
    active: Boolean,
    reason: String?,
  ): AutoAcceptRule {
    val sender = senderProfileRepository.findById(senderProfileId)
      .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SenderProfile $senderProfileId not found") }
    val prisoner = prisonerProfileRepository.findById(prisonerProfileId)
      .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "PrisonerProfile $prisonerProfileId not found") }

    val existing = autoAcceptRuleRepository.findBySenderProfileAndPrisonerProfile(sender, prisoner)
    if (existing != null) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Auto-accept rule already exists for this sender/prisoner pair")
    }

    val rule = AutoAcceptRule(senderProfile = sender, prisonerProfile = prisoner)
    val state = AutoAcceptRuleState(
      rule = rule,
      active = active,
      reason = reason,
      createdBy = createdBy,
    )
    rule.states.add(state)
    return autoAcceptRuleRepository.save(rule)
  }

  @Transactional
  fun patchAutoAcceptRule(
    id: Long,
    active: Boolean,
    reason: String?,
    createdBy: String?,
  ): AutoAcceptRule {
    val rule = autoAcceptRuleRepository.findById(id)
      .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "AutoAcceptRule $id not found") }

    val state = AutoAcceptRuleState(
      rule = rule,
      active = active,
      reason = reason,
      createdBy = createdBy,
    )
    rule.states.add(state)
    return autoAcceptRuleRepository.save(rule)
  }

  fun listAutoAcceptRules(
    isActive: Boolean? = null,
    senderProfileId: Long? = null,
    prisonerProfileId: Long? = null,
  ): List<AutoAcceptRule> {
    val all = when {
      senderProfileId != null -> autoAcceptRuleRepository.findBySenderProfileId(senderProfileId)
      prisonerProfileId != null -> autoAcceptRuleRepository.findByPrisonerProfileId(prisonerProfileId)
      else -> autoAcceptRuleRepository.findAll()
    }
    return if (isActive != null) {
      all.filter { it.isActive() == isActive }
    } else {
      all
    }
  }
}
