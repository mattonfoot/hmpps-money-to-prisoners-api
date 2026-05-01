package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * A log entry attached to a credit, disbursement, or other auditable record.
 *
 * Mirrors Python's `Log` schema. Used in the `logs` arrays of various DTOs.
 */
@Schema(name = "Log", description = "An audit log entry")
data class Log(
  @Schema(description = "Username of the user who performed the action", example = "clerk1")
  val user: String?,
  @Schema(description = "The action performed", example = "credited")
  val action: String,
  @Schema(description = "Timestamp of the log entry")
  val created: LocalDateTime?,
)
