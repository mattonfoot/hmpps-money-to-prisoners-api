package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRuleState
import java.time.LocalDateTime

@Schema(description = "An auto-accept rule for a sender/prisoner pair")
data class AutoAcceptRuleDto(
  val id: Long?,
  @JsonProperty("sender_profile")
  val senderProfile: Long,
  @JsonProperty("prisoner_profile")
  val prisonerProfile: Long,
  val states: List<AutoAcceptRuleStateDto>,
  @JsonProperty("is_active")
  val isActive: Boolean,
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(rule: AutoAcceptRule): AutoAcceptRuleDto = AutoAcceptRuleDto(
      id = rule.id,
      senderProfile = rule.senderProfile.id!!,
      prisonerProfile = rule.prisonerProfile.id!!,
      states = rule.states.map { AutoAcceptRuleStateDto.from(it) },
      isActive = rule.isActive(),
      created = rule.created,
      modified = rule.modified,
    )
  }
}

@Schema(description = "A state snapshot within an auto-accept rule")
data class AutoAcceptRuleStateDto(
  val id: Long?,
  val active: Boolean,
  val reason: String?,
  @JsonProperty("created_by")
  val createdBy: String?,
  val created: LocalDateTime?,
) {
  companion object {
    fun from(state: AutoAcceptRuleState): AutoAcceptRuleStateDto = AutoAcceptRuleStateDto(
      id = state.id,
      active = state.active,
      reason = state.reason,
      createdBy = state.createdBy,
      created = state.created,
    )
  }
}

@Schema(description = "Request to create or patch an auto-accept rule state")
data class AutoAcceptRuleStateRequest(
  val active: Boolean,
  val reason: String? = null,
)

@Schema(description = "Request body for creating an auto-accept rule")
data class CreateAutoAcceptRuleRequest(
  @JsonProperty("sender_profile")
  val senderProfile: Long,
  @JsonProperty("prisoner_profile")
  val prisonerProfile: Long,
  val states: List<AutoAcceptRuleStateRequest>,
)

@Schema(description = "Request body for patching an auto-accept rule (append state)")
data class PatchAutoAcceptRuleRequest(
  val states: List<AutoAcceptRuleStateRequest>,
)
