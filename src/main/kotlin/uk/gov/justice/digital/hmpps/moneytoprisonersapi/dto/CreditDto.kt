package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus.Companion.computeFrom
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "A credit record representing money sent to a prisoner")
data class CreditDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,
  @Schema(description = "Amount in pence", example = "5000")
  val amount: Long,
  @Schema(description = "Prisoner number (NOMIS ID)", example = "A1234BC")
  @JsonProperty("prisoner_number")
  val prisonerNumber: String?,
  @Schema(description = "Prisoner full name", example = "John Smith")
  @JsonProperty("prisoner_name")
  val prisonerName: String?,
  @Schema(description = "Prisoner date of birth", example = "1990-01-15")
  @JsonProperty("prisoner_dob")
  val prisonerDob: LocalDate?,
  @Schema(description = "Prison NOMIS ID where prisoner is located", example = "LEI")
  val prison: String?,
  @Schema(description = "Resolution status of the credit", example = "PENDING")
  val resolution: CreditResolution,
  @Schema(description = "Source type of the credit", example = "BANK_TRANSFER")
  val source: CreditSource,
  @Schema(description = "Computed display status derived from resolution, prison assignment, and blocked state", example = "credit_pending")
  val status: CreditStatus,
  @Schema(description = "Username of the clerk who credited the prisoner", example = "clerk1")
  val owner: String?,
  @Schema(description = "Full name of the owner/clerk", example = "John Clerk")
  @JsonProperty("owner_name")
  val ownerName: String?,
  @Schema(description = "Whether the credit is blocked", example = "false")
  val blocked: Boolean,
  @Schema(description = "Whether the credit has been reviewed by security staff", example = "false")
  val reviewed: Boolean,
  @Schema(description = "Whether the credit has been reconciled", example = "false")
  val reconciled: Boolean,
  @Schema(description = "Timestamp when the credit was received", example = "2024-03-15T10:30:00")
  @JsonProperty("received_at")
  val receivedAt: LocalDateTime?,
  @Schema(description = "Timestamp when the credit was started (created)", example = "2024-03-15T10:30:00")
  @JsonProperty("started_at")
  val startedAt: LocalDateTime?,
  @Schema(description = "Timestamp when the credit was credited (from log)", example = "2024-03-16T14:00:00")
  @JsonProperty("credited_at")
  val creditedAt: LocalDateTime?,
  @Schema(description = "Timestamp when the credit was refunded (from log)", example = "2024-03-17T09:00:00")
  @JsonProperty("refunded_at")
  val refundedAt: LocalDateTime?,
  @Schema(description = "Timestamp when the credit was set to manual (from log)", example = "2024-03-18T11:00:00")
  @JsonProperty("set_manual_at")
  val setManualAt: LocalDateTime?,
  @Schema(description = "Timestamp when the record was created", example = "2024-03-15T10:30:00")
  val created: LocalDateTime?,
  @Schema(description = "Timestamp when the record was last modified", example = "2024-03-15T10:30:00")
  val modified: LocalDateTime?,
  @Schema(description = "Name of the sender (from transaction)", example = "Alice Sender")
  @JsonProperty("sender_name")
  val senderName: String?,
  @Schema(description = "Email of the sender (from payment)", example = "alice@example.com")
  @JsonProperty("sender_email")
  val senderEmail: String?,
  @Schema(description = "First 8 characters of the payment UUID", example = "abcdef12")
  @JsonProperty("short_payment_ref")
  val shortPaymentRef: String?,
  @Schema(description = "True if credit has a transaction with incomplete sender info and is blocked", example = "false")
  val anonymous: Boolean,
  @Schema(description = "Intended recipient name from payment", example = "John Prisoner")
  @JsonProperty("intended_recipient")
  val intendedRecipient: String?,
  @Schema(description = "NOMIS transaction ID", example = "TXN001")
  @JsonProperty("nomis_transaction_id")
  val nomisTransactionId: String?,
  @Schema(description = "Reconciliation code")
  @JsonProperty("reconciliation_code")
  val reconciliationCode: String?,
  @Schema(description = "Comments on this credit")
  val comments: List<CommentDto>,
) {
  companion object {
    fun from(credit: Credit, ownerNameMap: Map<String, String> = emptyMap()): CreditDto = CreditDto(
      id = credit.id,
      amount = credit.amount,
      prisonerNumber = credit.prisonerNumber,
      prisonerName = credit.prisonerName,
      prisonerDob = credit.prisonerDob,
      prison = credit.prison,
      resolution = credit.resolution,
      source = credit.source,
      status = computeFrom(credit),
      owner = credit.owner,
      ownerName = credit.owner?.let { ownerNameMap[it] },
      blocked = credit.blocked,
      reviewed = credit.reviewed,
      reconciled = credit.reconciled,
      receivedAt = credit.receivedAt,
      startedAt = credit.created,
      creditedAt = credit.logs.firstOrNull { it.action == LogAction.CREDITED }?.created,
      refundedAt = credit.logs.firstOrNull { it.action == LogAction.REFUNDED }?.created,
      setManualAt = credit.logs.firstOrNull { it.action == LogAction.MANUAL }?.created,
      created = credit.created,
      modified = credit.modified,
      senderName = credit.transaction?.senderName,
      senderEmail = credit.payment?.email,
      shortPaymentRef = credit.payment?.uuid?.toString()?.replace("-", "")?.take(8),
      anonymous = credit.transaction?.incompleteSenderInfo == true && credit.blocked,
      intendedRecipient = credit.payment?.recipientName,
      nomisTransactionId = credit.nomisTransactionId,
      reconciliationCode = credit.transaction?.refCode?.let { "PMT$it" },
      comments = credit.comments.map { CommentDto.from(it) },
    )
  }
}
