package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import java.time.OffsetDateTime

@Schema(description = "An auto-accept rule for a sender/prisoner pair")
data class CheckAutoAcceptRule(
  val id: Long?,
  @JsonProperty("sender_profile")
  val senderProfile: Long,
  @JsonProperty("prisoner_profile")
  val prisonerProfile: Long,
  val states: List<CheckAutoAcceptRuleState>,
  @JsonProperty("is_active")
  val isActive: Boolean,
  val created: OffsetDateTime?,
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(rule: AutoAcceptRule): CheckAutoAcceptRule = CheckAutoAcceptRule(
      id = rule.id,
      // Django links the rule to a debit-card sender detail, not the parent
      // sender profile. Surface the parent profile id when the detail is loaded.
      senderProfile = rule.debitCardSenderDetails?.sender?.id ?: 0L,
      prisonerProfile = rule.prisonerProfile?.id ?: 0L,
      states = rule.states.sortedBy { it.created }.map { CheckAutoAcceptRuleState.from(it) },
      // Mirrors Django CheckAutoAcceptRule.is_active(): the .active flag on
      // the latest (most recently created) state row.
      isActive = rule.states.maxByOrNull { it.created }?.active ?: false,
      created = rule.created,
      modified = rule.modified,
    )
  }
}

@Schema(hidden = true)
data class AutoAcceptRuleStateRequest(
  val active: Boolean,
  val reason: String? = null,
)

@Schema(hidden = true)
data class CreateAutoAcceptRuleRequest(
  @JsonProperty("sender_profile")
  val senderProfile: Long,
  @JsonProperty("prisoner_profile")
  val prisonerProfile: Long,
  val states: List<AutoAcceptRuleStateRequest>,
)

@Schema(hidden = true)
data class PatchAutoAcceptRuleRequest(
  val states: List<AutoAcceptRuleStateRequest>,
)
