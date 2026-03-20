package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Per-service availability status returned by GET /service-availability/.
 *
 * SVC-002: Contains status (true = up, false = down).
 * SVC-004: Includes downtime_end and message_to_users when a downtime is active and those fields are set.
 * SVC-005: downtime_end is absent when the downtime has no scheduled end (ongoing).
 */
@Schema(description = "Availability status for a single service")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServiceStatusDto(
  @Schema(description = "True if the service is available, false if it is down", example = "true")
  val status: Boolean,

  @Schema(description = "ISO 8601 datetime when the downtime is scheduled to end; absent if ongoing or not down", nullable = true)
  @JsonProperty("downtime_end")
  val downtimeEnd: String? = null,

  @Schema(description = "Human-readable message to display to users during downtime; absent if not set", nullable = true)
  @JsonProperty("message_to_users")
  val messageToUsers: String? = null,
)
