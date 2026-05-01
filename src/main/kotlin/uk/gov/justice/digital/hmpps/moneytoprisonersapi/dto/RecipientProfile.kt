package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "A recipient profile aggregating disbursements to one bank account")
data class RecipientProfile(
  val id: Long?,
  @JsonProperty("sort_code")
  val sortCode: String?,
  @JsonProperty("account_number")
  val accountNumber: String?,
  @JsonProperty("monitoring_users")
  val monitoringUsers: List<String>,
  val monitoring: Boolean?,
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(profile: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.RecipientProfile, currentUsername: String? = null): RecipientProfile = RecipientProfile(
      id = profile.id,
      sortCode = profile.sortCode,
      accountNumber = profile.accountNumber,
      monitoringUsers = profile.monitoringUsers.toList(),
      monitoring = if (currentUsername != null) profile.monitoringUsers.contains(currentUsername) else null,
      created = profile.created,
      modified = profile.modified,
    )
  }
}
