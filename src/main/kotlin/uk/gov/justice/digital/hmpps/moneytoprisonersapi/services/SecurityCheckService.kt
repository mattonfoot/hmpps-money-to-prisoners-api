package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.SecurityCheckConflictException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRuleState
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AutoAcceptRuleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SecurityCheckRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SecurityDebitcardsenderdetailRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import java.time.OffsetDateTime

@Service
class SecurityCheckService(
  private val securityCheckRepository: SecurityCheckRepository,
  private val senderProfileRepository: SenderProfileRepository,
  private val prisonerProfileRepository: PrisonerProfileRepository,
  private val autoAcceptRuleRepository: AutoAcceptRuleRepository,
  private val userRepository: AuthUserRepository,
  private val debitCardSenderDetailRepository: SecurityDebitcardsenderdetailRepository,
) {
  private val authUserRepository: AuthUserRepository get() = userRepository

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
      // SecurityCheck.status is stored as a varchar of the enum's lowercase
      // `.value` ("pending", "accepted", "rejected"), not the enum itself.
      specs.add(Specification { root, _, cb -> cb.equal(root.get<String>("status"), status.value) })
    }

    if (rules != null) {
      // Django stores rules as a `varchar[]` column. The Python view does a
      // substring/contains match by serialising the array and `ILIKE %x%`. Mirror
      // that with the native equivalent: `array_to_string(rules,',') ILIKE %x%`.
      specs.add(
        Specification { root, query, cb ->
          val rulesText = cb.function(
            "array_to_string",
            String::class.java,
            root.get<Array<String>>("rules"),
            cb.literal(","),
          )
          cb.like(cb.lower(rulesText), "%${rules.lowercase()}%")
        },
      )
    }

    if (startedAtGte != null) {
      specs.add(
        Specification { root, _, cb ->
          cb.greaterThanOrEqualTo(root.get("actionedAt"), startedAtGte)
        },
      )
    }

    if (startedAtLt != null) {
      specs.add(
        Specification { root, _, cb ->
          cb.lessThan(root.get("actionedAt"), startedAtLt)
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

    // Django: CheckAutoAcceptRule has a FK to DebitCardSenderDetails (child of
    // SenderProfile), not the parent profile directly. Walk to the first detail
    // associated with this sender. Tests seed exactly one detail per sender.
    val detail = debitCardSenderDetailRepository.findBySender(sender).firstOrNull()
      ?: throw ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "SenderProfile $senderProfileId has no debit-card sender details to bind",
      )

    val createdByUser = createdBy?.let { authUserRepository.findByUsername(it) }
    val rule = AutoAcceptRule().apply {
      this.debitCardSenderDetails = detail
      this.prisonerProfile = prisoner
    }
    val state = AutoAcceptRuleState().apply {
      this.autoAcceptRule = rule
      this.active = active
      this.reason = reason ?: ""
      this.addedBy = createdByUser
    }
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
    // Django: PATCH appends a new CheckAutoAcceptRuleState row (history is preserved);
    // is_active() reads back the newest row.
    val newState = AutoAcceptRuleState().apply {
      this.autoAcceptRule = rule
      this.active = active
      this.reason = reason ?: ""
      this.addedBy = createdBy?.let { authUserRepository.findByUsername(it) }
    }
    rule.states.add(newState)
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
    if (isActive == null) return all
    // Django's CheckAutoAcceptRule.is_active() = states.order_by(-created).first().active
    return all.filter { rule ->
      val latest = rule.states.maxByOrNull { it.created }
      latest != null && latest.active == isActive
    }
  }
}
