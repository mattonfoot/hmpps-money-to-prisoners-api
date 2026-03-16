package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Balance
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "A daily closing balance record")
data class BalanceDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,
  @Schema(description = "Closing balance in pence", example = "12345")
  val closingBalance: BigInteger,
  @Schema(description = "Date of the balance record", example = "2024-01-15")
  val date: LocalDate,
  @Schema(description = "Timestamp when the record was created", example = "2024-01-15 10:30:00")
  val created: LocalDateTime?,
  @Schema(description = "Timestamp when the record was last modified", example = "2024-01-15 10:30:00")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(balance: Balance): BalanceDto = BalanceDto(
      id = balance.id,
      closingBalance = balance.closingBalance,
      date = balance.date,
      created = balance.created,
      modified = balance.modified,
    )
  }
}
