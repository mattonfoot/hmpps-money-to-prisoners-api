package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PaymentBatch
import java.time.LocalDate
import java.time.OffsetDateTime

@Schema(description = "A payment reconciliation batch")
data class Batch(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,

  @Schema(description = "Auto-incremented reference code", example = "1")
  @JsonProperty("ref_code")
  val refCode: String,

  @Schema(description = "Settlement date for the batch", example = "2024-01-31")
  @JsonProperty("settlement_date")
  val settlementDate: LocalDate?,

  @Schema(description = "Total payment amount in pence for all payments in this batch", example = "15000")
  @JsonProperty("payment_amount")
  val paymentAmount: Long,

  @Schema(description = "Timestamp when the batch was created", example = "2024-01-31T12:00:00")
  val created: OffsetDateTime?,
) {
  companion object {
    fun from(batch: PaymentBatch): Batch = Batch(
      id = batch.id,
      refCode = batch.refCode,
      // Django models the date as `date`; the legacy DTO surfaces `settlement_date`.
      settlementDate = batch.date,
      // Django wires payments to the batch via payment_payment.batch_id. Computing
      // a total here would require loading children — left as 0 until callers
      // need it (the legacy aggregation can be moved into a service helper).
      paymentAmount = 0L,
      created = batch.created,
    )
  }
}
