package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Event
import java.time.LocalDateTime

@Schema(description = "A notification event")
data class EventDto(
  @Schema(description = "Event ID", example = "1")
  val id: Long?,

  @JsonProperty("credit_id")
  @Schema(description = "Linked credit ID, or null", nullable = true)
  val creditId: Long?,

  @JsonProperty("disbursement_id")
  @Schema(description = "Linked disbursement ID, or null", nullable = true)
  val disbursementId: Long?,

  @JsonProperty("sender_profile")
  @Schema(description = "Linked sender profile ID, or null", nullable = true)
  val senderProfile: Long?,

  @JsonProperty("prisoner_profile")
  @Schema(description = "Linked prisoner profile ID, or null", nullable = true)
  val prisonerProfile: Long?,

  @JsonProperty("triggered_at")
  @Schema(description = "When the event was triggered", nullable = true)
  val triggeredAt: LocalDateTime?,

  @Schema(description = "Rule code that triggered this event", example = "MONP")
  val rule: String,

  @Schema(description = "Human-readable description of the rule")
  val description: String,
) {
  companion object {
    fun from(event: Event): EventDto = EventDto(
      id = event.id,
      creditId = event.credit?.id,
      disbursementId = event.disbursement?.id,
      senderProfile = event.senderProfile?.id,
      prisonerProfile = event.prisonerProfile?.id,
      triggeredAt = event.triggeredAt,
      rule = event.rule,
      description = event.description,
    )
  }
}
