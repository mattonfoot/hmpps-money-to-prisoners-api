package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Date-based pagination summary for notification events")
data class EventPagesResponse(
  @Schema(description = "Newest date with events", nullable = true)
  val newest: LocalDate?,

  @Schema(description = "Oldest date with events", nullable = true)
  val oldest: LocalDate?,

  @Schema(description = "Total number of distinct dates with events")
  val count: Int,
)
