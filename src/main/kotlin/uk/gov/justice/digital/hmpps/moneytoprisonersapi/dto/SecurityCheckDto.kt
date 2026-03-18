package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import java.time.LocalDateTime

@Schema(description = "A security check performed on a credit")
data class SecurityCheckDto(
  @Schema(description = "Unique identifier", example = "10")
  val id: Long?,
  @Schema(description = "Check status", example = "ACCEPTED")
  val status: CheckStatus,
  @Schema(description = "Description of the security check", example = "Verified sender")
  val description: String?,
  @Schema(description = "Reason for the decision", example = "Known sender")
  @JsonProperty("decision_reason")
  val decisionReason: String?,
  @Schema(description = "User who actioned the check", example = "security_user")
  @JsonProperty("actioned_by")
  val actionedBy: String?,
  @Schema(description = "Timestamp when the check was actioned", example = "2024-03-16T14:00:00")
  @JsonProperty("actioned_at")
  val actionedAt: LocalDateTime?,
  @Schema(description = "Timestamp when the check was created", example = "2024-03-15T10:30:00")
  val created: LocalDateTime?,
  @Schema(description = "Timestamp when the check was last modified", example = "2024-03-15T10:30:00")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(securityCheck: SecurityCheck): SecurityCheckDto = SecurityCheckDto(
      id = securityCheck.id,
      status = securityCheck.status,
      description = securityCheck.description,
      decisionReason = securityCheck.decisionReason,
      actionedBy = securityCheck.actionedBy,
      actionedAt = securityCheck.actionedAt,
      created = securityCheck.created,
      modified = securityCheck.modified,
    )
  }
}
