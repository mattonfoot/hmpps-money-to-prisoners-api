package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "One week of performance data with percentages pre-formatted as strings")
data class PerformanceDataDto(
  @Schema(description = "Week start date (Monday)", example = "2024-01-01")
  val week: LocalDate,

  @Schema(description = "Total number of credits in the week", example = "150", nullable = true)
  val creditsTotal: Int?,

  @Schema(description = "Number of credits made via MTP in the week", example = "120", nullable = true)
  val creditsByMtp: Int?,

  @Schema(description = "Digital take-up as a formatted percentage, e.g. \"80%\"", example = "80%", nullable = true)
  val digitalTakeup: String?,

  @Schema(description = "Completion rate as a formatted percentage, e.g. \"95%\"", example = "95%", nullable = true)
  val completionRate: String?,

  @Schema(description = "User satisfaction as a formatted percentage, e.g. \"67%\"", example = "67%", nullable = true)
  val userSatisfaction: String?,

  @Schema(description = "Count of very dissatisfied ratings", nullable = true)
  val rated1: Int? = null,

  @Schema(description = "Count of dissatisfied ratings", nullable = true)
  val rated2: Int? = null,

  @Schema(description = "Count of neutral ratings", nullable = true)
  val rated3: Int? = null,

  @Schema(description = "Count of satisfied ratings", nullable = true)
  val rated4: Int? = null,

  @Schema(description = "Count of very satisfied ratings", nullable = true)
  val rated5: Int? = null,
)

@Schema(description = "Performance data response with field headers and weekly results")
data class PerformanceDataResponse(
  @Schema(description = "Map of field name to verbose label, suitable for CSV column headers")
  val headers: Map<String, String>,

  @Schema(description = "List of weekly performance records")
  val results: List<PerformanceDataDto>,
)
