package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "A prisoner profile aggregating credits for one prisoner")
data class PrisonerProfileDto(
  val id: Long?,
  @JsonProperty("prisoner_number")
  val prisonerNumber: String?,
  @JsonProperty("prisoner_name")
  val prisonerName: String?,
  @JsonProperty("prisoner_dob")
  val prisonerDob: LocalDate?,
  @JsonProperty("credit_count")
  val creditCount: Int,
  @JsonProperty("credit_total")
  val creditTotal: Long,
  @JsonProperty("sender_count")
  val senderCount: Int,
  @JsonProperty("recipient_count")
  val recipientCount: Int,
  @JsonProperty("disbursement_count")
  val disbursementCount: Int,
  @JsonProperty("disbursement_total")
  val disbursementTotal: Long,
  val prisons: List<String>,
  @JsonProperty("current_prison")
  val currentPrison: String?,
  @JsonProperty("provided_names")
  val providedNames: List<String>,
  @JsonProperty("monitoring_users")
  val monitoringUsers: List<String>,
  val monitoring: Boolean?,
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(profile: PrisonerProfile, currentUsername: String? = null): PrisonerProfileDto = PrisonerProfileDto(
      id = profile.id,
      prisonerNumber = profile.prisonerNumber,
      prisonerName = profile.prisonerName,
      prisonerDob = null,
      creditCount = profile.credits.size,
      creditTotal = profile.credits.sumOf { it.amount },
      senderCount = 0,
      recipientCount = 0,
      disbursementCount = 0,
      disbursementTotal = 0,
      prisons = profile.credits.mapNotNull { it.prison }.distinct(),
      currentPrison = null,
      providedNames = profile.credits.mapNotNull { it.prisonerName }.distinct(),
      monitoringUsers = profile.monitoringUsers.toList(),
      monitoring = if (currentUsername != null) profile.monitoringUsers.contains(currentUsername) else null,
      created = profile.created,
      modified = profile.modified,
    )
  }
}
