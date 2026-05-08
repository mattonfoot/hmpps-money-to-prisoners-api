package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRuleState
import java.time.OffsetDateTime

@Schema(description = "A state snapshot within an auto-accept rule")
data class CheckAutoAcceptRuleState(
  val id: Long?,
  val active: Boolean,
  val reason: String?,
  @JsonProperty("created_by")
  val createdBy: String?,
  val created: OffsetDateTime?,
) {
  companion object {
    fun from(state: AutoAcceptRuleState): CheckAutoAcceptRuleState = CheckAutoAcceptRuleState(
      id = state.id,
      active = state.active,
      reason = state.reason,
      createdBy = state.addedBy?.username,
      created = state.created,
    )
  }
}
