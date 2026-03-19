package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import java.time.LocalDateTime

@Schema(description = "A prisoner profile aggregating credits for one prisoner")
data class PrisonerProfileDto(
  val id: Long?,
  @JsonProperty("prisoner_number")
  val prisonerNumber: String?,
  @JsonProperty("prisoner_name")
  val prisonerName: String?,
  @JsonProperty("credit_count")
  val creditCount: Int,
  @JsonProperty("sender_count")
  val senderCount: Int,
  @JsonProperty("monitoring_users")
  val monitoringUsers: List<String>,
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(profile: PrisonerProfile): PrisonerProfileDto = PrisonerProfileDto(
      id = profile.id,
      prisonerNumber = profile.prisonerNumber,
      prisonerName = profile.prisonerName,
      creditCount = profile.credits.size,
      senderCount = 0,
      monitoringUsers = profile.monitoringUsers.toList(),
      created = profile.created,
      modified = profile.modified,
    )
  }
}
