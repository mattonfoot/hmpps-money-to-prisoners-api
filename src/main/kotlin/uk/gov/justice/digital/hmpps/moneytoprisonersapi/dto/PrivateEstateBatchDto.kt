package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import java.time.LocalDate

@Schema(description = "A private estate batch grouping credits for a private prison on a specific date")
data class PrivateEstateBatchDto(
  @Schema(description = "Batch reference (format: PRISON/YYYY-MM-DD)", example = "PRV/2024-03-15")
  val ref: String,
  @Schema(description = "Prison NOMIS ID", example = "PRV")
  val prison: String,
  @Schema(description = "Date of the batch", example = "2024-03-15")
  val date: LocalDate,
  @Schema(description = "Total amount in pence for all credits in this batch", example = "50000")
  @JsonProperty("total_amount")
  val totalAmount: Long,
  @Schema(description = "Number of credits in this batch", example = "5")
  val count: Int,
) {
  companion object {
    fun from(batch: PrivateEstateBatch): PrivateEstateBatchDto = PrivateEstateBatchDto(
      ref = batch.ref,
      prison = batch.prison,
      date = batch.date,
      totalAmount = batch.totalAmount,
      count = batch.credits.size,
    )
  }
}
