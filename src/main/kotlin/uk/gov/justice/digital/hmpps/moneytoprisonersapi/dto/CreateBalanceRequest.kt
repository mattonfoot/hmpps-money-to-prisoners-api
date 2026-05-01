package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigInteger
import java.time.LocalDate

@Schema(hidden = true)
data class CreateBalanceRequest(
  @Schema(description = "Closing balance in pence", example = "12345", required = true)
  val closingBalance: BigInteger,
  @Schema(description = "Date of the balance record", example = "2024-01-15", required = true)
  val date: LocalDate,
)
