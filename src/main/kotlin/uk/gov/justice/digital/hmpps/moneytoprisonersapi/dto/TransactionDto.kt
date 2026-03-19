package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.TransactionStatus
import java.time.LocalDateTime

@Schema(description = "A bank transfer transaction record")
data class TransactionDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,
  @Schema(description = "Amount in pence", example = "5000")
  val amount: Long,
  @Schema(description = "Transaction category (credit or debit)", example = "credit")
  val category: TransactionCategory,
  @Schema(description = "Transaction source", example = "bank_transfer")
  val source: TransactionSource,
  @Schema(description = "Computed status derived from credit linkage, sender info, and prison assignment", example = "creditable")
  val status: TransactionStatus,
  @Schema(description = "Bank sort code of the sender", example = "112233")
  @JsonProperty("sender_sort_code")
  val senderSortCode: String?,
  @Schema(description = "Bank account number of the sender", example = "12345678")
  @JsonProperty("sender_account_number")
  val senderAccountNumber: String?,
  @Schema(description = "Name of the sender", example = "Alice Sender")
  @JsonProperty("sender_name")
  val senderName: String?,
  @Schema(description = "Building society roll number of the sender", example = "ROLL001")
  @JsonProperty("sender_roll_number")
  val senderRollNumber: String?,
  @Schema(description = "Payment reference text", example = "REF001")
  val reference: String?,
  @Schema(description = "Timestamp when the payment was received", example = "2024-01-15T10:30:00")
  @JsonProperty("received_at")
  val receivedAt: LocalDateTime?,
  @Schema(description = "6-digit reconciliation reference code", example = "REF001")
  @JsonProperty("ref_code")
  val refCode: String?,
  @Schema(description = "Whether sender information is incomplete", example = "false")
  @JsonProperty("incomplete_sender_info")
  val incompleteSenderInfo: Boolean,
  @Schema(description = "Whether the reference appears in the sender field", example = "false")
  @JsonProperty("reference_in_sender_field")
  val referenceInSenderField: Boolean,
  @Schema(description = "Optional payment processor type code", example = "BACS")
  @JsonProperty("processor_type_code")
  val processorTypeCode: String?,
  @Schema(description = "ID of the linked credit, if any", example = "42")
  @JsonProperty("credit")
  val creditId: Long?,
  @Schema(description = "Timestamp when the record was created", example = "2024-01-15T10:30:00")
  val created: LocalDateTime?,
  @Schema(description = "Timestamp when the record was last modified", example = "2024-01-15T10:30:00")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(transaction: Transaction): TransactionDto = TransactionDto(
      id = transaction.id,
      amount = transaction.amount,
      category = transaction.category,
      source = transaction.source,
      status = TransactionStatus.computeFrom(transaction),
      senderSortCode = transaction.senderSortCode,
      senderAccountNumber = transaction.senderAccountNumber,
      senderName = transaction.senderName,
      senderRollNumber = transaction.senderRollNumber,
      reference = transaction.reference,
      receivedAt = transaction.receivedAt,
      refCode = transaction.refCode,
      incompleteSenderInfo = transaction.incompleteSenderInfo,
      referenceInSenderField = transaction.referenceInSenderField,
      processorTypeCode = transaction.processorTypeCode,
      creditId = transaction.credit?.id,
      created = transaction.created,
      modified = transaction.modified,
    )
  }
}
