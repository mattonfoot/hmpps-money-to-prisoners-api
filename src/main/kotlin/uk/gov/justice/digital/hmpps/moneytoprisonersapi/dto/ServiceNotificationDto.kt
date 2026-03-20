package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.ServiceNotification
import java.time.LocalDateTime

/** Django message level integers mapped to their string labels. */
private val LEVEL_LABELS = mapOf(
  20 to "info",
  25 to "success",
  30 to "warning",
  40 to "error",
)

/**
 * API representation of a [ServiceNotification].
 *
 * SVC-013: level is returned as a string label (info, success, warning, error).
 */
@Schema(description = "A banner/alert notification shown to users in a front-end application")
data class ServiceNotificationDto(
  @Schema(description = "Target app/view for this notification (e.g. cashbook_login, noms_ops_security_dashboard)", example = "cashbook_login")
  val target: String,

  @Schema(description = "Severity level label: info, success, warning, or error", example = "warning")
  val level: String,

  @Schema(description = "When the notification becomes active")
  val start: LocalDateTime,

  @Schema(description = "When the notification expires; null means no scheduled end", nullable = true)
  val end: LocalDateTime?,

  @Schema(description = "Short headline displayed to users", example = "Planned maintenance tonight")
  val headline: String,

  @Schema(description = "Optional longer message body")
  val message: String,

  @JsonIgnore
  val public: Boolean = false,
) {
  companion object {
    fun from(entity: ServiceNotification): ServiceNotificationDto = ServiceNotificationDto(
      target = entity.target,
      level = LEVEL_LABELS[entity.level] ?: "error",
      start = entity.start,
      end = entity.end,
      headline = entity.headline,
      message = entity.message,
      public = entity.public,
    )
  }
}
