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

@Schema(description = "A credit record with additional security-related bank and card details")
data class SecurityCreditDto(
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
  @Schema(description = "Computed display status", example = "credit_pending")
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
  @Schema(description = "Comments on this credit")
  val comments: List<CommentDto>,

  // Security-specific fields (CRD-106)
  @Schema(description = "Sender sort code (from transaction)", example = "112233")
  @JsonProperty("sender_sort_code")
  val senderSortCode: String?,
  @Schema(description = "Sender account number (from transaction)", example = "12345678")
  @JsonProperty("sender_account_number")
  val senderAccountNumber: String?,
  @Schema(description = "Sender roll number (from transaction)", example = "ROLL001")
  @JsonProperty("sender_roll_number")
  val senderRollNumber: String?,
  @Schema(description = "First digits of the card number (from payment)", example = "411111")
  @JsonProperty("card_number_first_digits")
  val cardNumberFirstDigits: String?,
  @Schema(description = "Last digits of the card number (from payment)", example = "1234")
  @JsonProperty("card_number_last_digits")
  val cardNumberLastDigits: String?,
  @Schema(description = "Card expiry date (from payment)", example = "12/25")
  @JsonProperty("card_expiry_date")
  val cardExpiryDate: String?,
  @Schema(description = "Sender IP address (from payment)", example = "192.168.1.1")
  @JsonProperty("sender_ip_address")
  val senderIpAddress: String?,
  @Schema(description = "Billing address associated with the payment")
  @JsonProperty("billing_address")
  val billingAddress: BillingAddressDto?,

  // Profile PKs (CRD-107)
  @Schema(description = "Sender profile ID", example = "42")
  @JsonProperty("sender_profile")
  val senderProfile: Long?,
  @Schema(description = "Prisoner profile ID", example = "99")
  @JsonProperty("prisoner_profile")
  val prisonerProfile: Long?,
) {
  companion object {
    fun from(
      credit: Credit,
      senderProfileId: Long? = null,
      prisonerProfileId: Long? = null,
    ): SecurityCreditDto = SecurityCreditDto(
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
      ownerName = null,
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
      comments = credit.comments.map { CommentDto.from(it) },
      senderSortCode = credit.transaction?.senderSortCode,
      senderAccountNumber = credit.transaction?.senderAccountNumber,
      senderRollNumber = credit.transaction?.senderRollNumber,
      cardNumberFirstDigits = credit.payment?.cardNumberFirstDigits,
      cardNumberLastDigits = credit.payment?.cardNumberLastDigits,
      cardExpiryDate = credit.payment?.cardExpiryDate,
      senderIpAddress = credit.payment?.ipAddress,
      billingAddress = credit.payment?.billingAddress?.let { BillingAddressDto.from(it) },
      senderProfile = senderProfileId,
      prisonerProfile = prisonerProfileId,
    )
  }
}
