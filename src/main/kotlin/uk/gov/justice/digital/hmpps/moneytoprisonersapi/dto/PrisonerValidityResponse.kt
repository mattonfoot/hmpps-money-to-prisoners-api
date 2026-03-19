package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Prisoner validity check response")
data class PrisonerValidityResponse(
  @Schema(description = "Count of matching active prisoner locations", example = "1")
  val count: Int,

  @Schema(description = "List of matching locations (simplified)")
  val results: List<Map<String, String>>,
)
