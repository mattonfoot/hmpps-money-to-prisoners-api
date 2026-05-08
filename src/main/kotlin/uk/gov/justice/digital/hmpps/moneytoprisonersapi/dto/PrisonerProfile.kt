package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import java.time.LocalDate
import java.time.OffsetDateTime

@Schema(description = "A prisoner profile aggregating credits for one prisoner")
data class PrisonerProfile(
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
  val created: OffsetDateTime?,
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(
      profile: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile,
      currentUsername: String? = null,
      disbursements: List<Disbursement> = emptyList(),
      senderCount: Int? = null,
      recipientCount: Int? = null,
    ): PrisonerProfile {
      val credits = profile.credits
      val mostCommonDob = credits.mapNotNull { it.prisonerDob }
        .groupingBy { it }.eachCount()
        .maxByOrNull { it.value }?.key
      val latestPrison = credits.filter { it.prison != null }
        .maxByOrNull { it.created }?.prison
      return PrisonerProfile(
        id = profile.id,
        prisonerNumber = profile.prisonerNumber,
        prisonerName = profile.prisonerName,
        prisonerDob = mostCommonDob,
        creditCount = credits.size,
        creditTotal = credits.sumOf { it.amount },
        senderCount = senderCount ?: 0,
        recipientCount = recipientCount ?: 0,
        disbursementCount = disbursements.size,
        disbursementTotal = disbursements.sumOf { it.amount.toLong() },
        prisons = credits.mapNotNull { it.prison?.nomisId }.distinct(),
        currentPrison = latestPrison?.nomisId,
        providedNames = credits.mapNotNull { it.prisonerName }.distinct(),
        // monitoringUsers are stored as auth_user IDs in Django; the legacy
        // contract surfaces usernames. Conversion lives in the service layer
        // — emit empty here to keep the DTO schema-stable.
        monitoringUsers = emptyList(),
        monitoring = null,
        created = profile.created,
        modified = profile.modified,
      )
    }
  }
}
