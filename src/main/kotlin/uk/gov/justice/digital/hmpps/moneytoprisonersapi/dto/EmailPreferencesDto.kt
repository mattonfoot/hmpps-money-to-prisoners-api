package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "User email notification frequency preference")
data class EmailPreferencesDto(
  @Schema(description = "Email notification frequency", example = "never", allowableValues = ["daily", "never"])
  val frequency: String,
)

@Schema(description = "Request body to set email notification frequency")
data class SetEmailPreferencesRequest(
  @Schema(description = "Desired email frequency", example = "daily", allowableValues = ["daily", "never"])
  val frequency: String?,
)
