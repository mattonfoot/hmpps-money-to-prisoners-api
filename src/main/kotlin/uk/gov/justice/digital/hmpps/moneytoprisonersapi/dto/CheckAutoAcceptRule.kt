package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import java.time.LocalDateTime

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
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(rule: AutoAcceptRule): CheckAutoAcceptRule = CheckAutoAcceptRule(
      id = rule.id,
      senderProfile = rule.senderProfile.id!!,
      prisonerProfile = rule.prisonerProfile.id!!,
      states = rule.states.map { CheckAutoAcceptRuleState.from(it) },
      isActive = rule.isActive(),
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
