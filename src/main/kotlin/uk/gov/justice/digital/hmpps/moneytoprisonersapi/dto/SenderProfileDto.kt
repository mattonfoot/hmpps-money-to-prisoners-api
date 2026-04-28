package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import java.time.LocalDateTime

@Schema(description = "A sender profile aggregating credits from one sender")
data class SenderProfileDto(
  val id: Long?,
  @JsonProperty("credit_count")
  val creditCount: Int,
  @JsonProperty("credit_total")
  val creditTotal: Long,
  @JsonProperty("prisoner_count")
  val prisonerCount: Int,
  @JsonProperty("prison_count")
  val prisonCount: Int,
  val prisons: List<String>,
  @JsonProperty("bank_transfer_details")
  val bankTransferDetails: List<Map<String, Any?>>,
  @JsonProperty("debit_card_details")
  val debitCardDetails: List<Map<String, Any?>>,
  @JsonProperty("monitoring_users")
  val monitoringUsers: List<String>,
  val monitoring: Boolean?,
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(profile: SenderProfile, currentUsername: String? = null): SenderProfileDto = SenderProfileDto(
      id = profile.id,
      creditCount = profile.credits.size,
      creditTotal = profile.credits.sumOf { it.amount },
      prisonerCount = profile.credits.mapNotNull { it.prisonerNumber }.distinct().size,
      prisonCount = profile.credits.mapNotNull { it.prison }.distinct().size,
      prisons = profile.credits.mapNotNull { it.prison }.distinct(),
      bankTransferDetails = emptyList(),
      debitCardDetails = emptyList(),
      monitoringUsers = profile.monitoringUsers.toList(),
      monitoring = if (currentUsername != null) profile.monitoringUsers.contains(currentUsername) else null,
      created = profile.created,
      modified = profile.modified,
    )
  }
}
