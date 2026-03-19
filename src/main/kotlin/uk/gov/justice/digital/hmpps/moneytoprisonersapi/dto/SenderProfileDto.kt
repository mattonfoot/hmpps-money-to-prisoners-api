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
  @JsonProperty("prisoner_count")
  val prisonerCount: Int,
  @JsonProperty("monitoring_users")
  val monitoringUsers: List<String>,
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(profile: SenderProfile, currentUsername: String? = null): SenderProfileDto = SenderProfileDto(
      id = profile.id,
      creditCount = profile.credits.size,
      prisonerCount = profile.credits.mapNotNull { it.prisonerNumber }.distinct().size,
      monitoringUsers = profile.monitoringUsers.toList(),
      created = profile.created,
      modified = profile.modified,
    )
  }
}
