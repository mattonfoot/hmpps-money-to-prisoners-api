package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "An enabled notification rule")
data class RuleDto(
  @Schema(description = "Rule code", example = "MONP")
  val code: String,

  @Schema(description = "Human-readable description of the rule")
  val description: String,
)
