package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionSource
import java.time.LocalDateTime

@Schema(description = "Request body for creating a bank transfer transaction")
data class CreateTransactionRequest(
  @Schema(description = "Amount in pence", example = "5000", required = true)
  @field:NotNull
  val amount: Long,

  @Schema(description = "Transaction category (credit or debit)", example = "credit", required = true)
  @field:NotNull
  val category: TransactionCategory,

  @Schema(description = "Transaction source", example = "bank_transfer", required = true)
  @field:NotNull
  val source: TransactionSource,

  @Schema(description = "Bank sort code of the sender", example = "112233")
  @JsonProperty("sender_sort_code")
  val senderSortCode: String? = null,

  @Schema(description = "Bank account number of the sender", example = "12345678")
  @JsonProperty("sender_account_number")
  val senderAccountNumber: String? = null,

  @Schema(description = "Name of the sender", example = "Alice Sender")
  @JsonProperty("sender_name")
  val senderName: String? = null,

  @Schema(description = "Building society roll number of the sender", example = "ROLL001")
  @JsonProperty("sender_roll_number")
  val senderRollNumber: String? = null,

  @Schema(description = "Payment reference text")
  val reference: String? = null,

  @Schema(description = "Timestamp when the payment was received", example = "2024-01-15T10:30:00")
  @JsonProperty("received_at")
  val receivedAt: LocalDateTime? = null,

  @Schema(description = "Reconciliation reference code", example = "REF001")
  @JsonProperty("ref_code")
  val refCode: String? = null,

  @Schema(description = "Whether sender information is incomplete", example = "false")
  @JsonProperty("incomplete_sender_info")
  val incompleteSenderInfo: Boolean = false,

  @Schema(description = "Whether the reference appears in the sender field", example = "false")
  @JsonProperty("reference_in_sender_field")
  val referenceInSenderField: Boolean = false,

  @Schema(description = "Optional payment processor type code", example = "BACS")
  @JsonProperty("processor_type_code")
  val processorTypeCode: String? = null,
)
