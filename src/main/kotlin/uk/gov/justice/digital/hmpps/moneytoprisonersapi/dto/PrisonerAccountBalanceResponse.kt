package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "PrisonerAccountBalance", description = "Prisoner account balance response")
data class PrisonerAccountBalanceResponse(
  @Schema(description = "Combined account balance in pence", example = "5000")
  val combinedAccountBalance: Long,
)
