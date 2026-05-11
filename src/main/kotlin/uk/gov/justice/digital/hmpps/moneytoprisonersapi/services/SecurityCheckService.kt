package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.SecurityCheckConflictException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AutoAcceptRuleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SecurityCheckRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import java.time.OffsetDateTime

@Service
class SecurityCheckService(
  private val securityCheckRepository: SecurityCheckRepository,
  private val senderProfileRepository: SenderProfileRepository,
  private val prisonerProfileRepository: PrisonerProfileRepository,
  private val autoAcceptRuleRepository: AutoAcceptRuleRepository,
  private val userRepository: AuthUserRepository,
) {

  @Transactional
  fun acceptCheck(id: Long, username: String, decisionReason: String) {
    val check = securityCheckRepository.findById(id)
      .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SecurityCheck $id not found") }

    when (CheckStatus.fromValue(check.status)) {
      CheckStatus.ACCEPTED -> return

      // idempotent
      CheckStatus.REJECTED -> throw SecurityCheckConflictException("Cannot accept a check that is already rejected")

      CheckStatus.PENDING -> {
        check.status = CheckStatus.ACCEPTED.value
        check.decisionReason = decisionReason
        check.actionedBy = userRepository.findByUsername(username)
        check.actionedAt = OffsetDateTime.now()
        securityCheckRepository.save(check)
      }
    }
  }

  @Transactional
  fun rejectCheck(id: Long, username: String, decisionReason: String, rejectionReasons: List<String>) {
    val check = securityCheckRepository.findById(id)
      .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SecurityCheck $id not found") }

    when (CheckStatus.fromValue(check.status)) {
      CheckStatus.REJECTED -> return

      // idempotent
      CheckStatus.ACCEPTED -> throw SecurityCheckConflictException("Cannot reject a check that is already accepted")

      CheckStatus.PENDING -> {
        check.status = CheckStatus.REJECTED.value
        check.decisionReason = decisionReason
        check.rejectionReasons = mutableMapOf<String, Any>("reasons" to rejectionReasons)
        check.actionedBy = userRepository.findByUsername(username)
        check.actionedAt = OffsetDateTime.now()
        securityCheckRepository.save(check)
      }
    }
  }

  fun getCheck(id: Long): SecurityCheck = securityCheckRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SecurityCheck $id not found") }

  @Transactional
  fun patchCheck(id: Long, assignedTo: String?): SecurityCheck {
    val check = getCheck(id)
    val currentAssignee = check.assignedTo?.username
    if (assignedTo != null && currentAssignee != null) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Check is already assigned to $currentAssignee")
    }
    check.assignedTo = assignedTo?.let { userRepository.findByUsername(it) }
    return securityCheckRepository.save(check)
  }

  fun listChecks(
    status: CheckStatus? = null,
    rules: String? = null,
    startedAtGte: OffsetDateTime? = null,
    startedAtLt: OffsetDateTime? = null,
    actionedByIsNull: Boolean? = null,
    creditResolution: String? = null,
  ): List<SecurityCheck> {
    val spec = buildSpecification(status, rules, startedAtGte, startedAtLt, actionedByIsNull, creditResolution)
    return securityCheckRepository.findAll(spec)
  }

  private fun buildSpecification(
    status: CheckStatus?,
    rules: String?,
    startedAtGte: OffsetDateTime?,
    startedAtLt: OffsetDateTime?,
    actionedByIsNull: Boolean?,
    creditResolution: String?,
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

    if (actionedByIsNull != null) {
      specs.add(
        Specification { root, _, cb ->
          if (actionedByIsNull) {
            cb.isNull(root.get<String>("actionedBy"))
          } else {
            cb.isNotNull(root.get<String>("actionedBy"))
          }
        },
      )
    }

    if (creditResolution != null) {
      specs.add(
        Specification { root, _, cb ->
          val creditJoin = root.join<SecurityCheck, Any>("credit", jakarta.persistence.criteria.JoinType.LEFT)
          cb.equal(cb.upper(creditJoin.get("resolution")), creditResolution.uppercase())
        },
      )
    }

    if (specs.isEmpty()) return Specification { _, _, cb -> cb.conjunction() }
    return specs.reduce { acc, s -> acc.and(s) }
  }

  fun getAutoAcceptRule(id: Long): AutoAcceptRule = autoAcceptRuleRepository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

  @Transactional
  fun createAutoAcceptRule(
    senderProfileId: Long,
    prisonerProfileId: Long,
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

    // Django models AutoAcceptRule with FKs to debit_card_sender_details + prisoner
    // profile, and stores state history in security_checkautoacceptrulestate. The
    // existing senderProfile-based path needs to walk to the debit-card detail
    // child first. Re-implement against Django's shape.
    TODO("re-implement createAutoAcceptRule against Django's checkautoacceptrule shape")
  }

  @Transactional
  fun patchAutoAcceptRule(
    id: Long,
    active: Boolean,
    reason: String?,
    createdBy: String?,
  ): AutoAcceptRule {
    @Suppress("UNUSED_VARIABLE")
    val rule = autoAcceptRuleRepository.findById(id)
      .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "AutoAcceptRule $id not found") }
    TODO("re-implement patchAutoAcceptRule against Django's checkautoacceptrulestate shape")
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
    // isActive filter requires walking the latest state row in
    // security_checkautoacceptrulestate. Stubbing for now.
    return all
  }
}
