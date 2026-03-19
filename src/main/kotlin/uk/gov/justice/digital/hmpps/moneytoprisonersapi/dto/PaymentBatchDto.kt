package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PaymentBatch
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "A payment reconciliation batch")
data class PaymentBatchDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,

  @Schema(description = "Auto-incremented reference code", example = "1")
  @JsonProperty("ref_code")
  val refCode: Int,

  @Schema(description = "Settlement date for the batch", example = "2024-01-31")
  @JsonProperty("settlement_date")
  val settlementDate: LocalDate?,

  @Schema(description = "Total payment amount in pence for all payments in this batch", example = "15000")
  @JsonProperty("payment_amount")
  val paymentAmount: Long,

  @Schema(description = "Timestamp when the batch was created", example = "2024-01-31T12:00:00")
  val created: LocalDateTime?,
) {
  companion object {
    fun from(batch: PaymentBatch): PaymentBatchDto {
      val paymentAmount = batch.credits.sumOf { credit ->
        credit.payment?.amount ?: 0L
      }
      return PaymentBatchDto(
        id = batch.id,
        refCode = batch.refCode,
        settlementDate = batch.settlementDate,
        paymentAmount = paymentAmount,
        created = batch.created,
      )
    }
  }
}
