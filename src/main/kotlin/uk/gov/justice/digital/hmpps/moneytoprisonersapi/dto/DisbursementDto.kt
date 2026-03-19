package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import java.time.LocalDateTime

@Schema(description = "A disbursement record representing money sent out from a prisoner account")
data class DisbursementDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,

  @Schema(description = "Amount in pence", example = "5000")
  val amount: Long,

  @Schema(description = "Payment method", example = "BANK_TRANSFER")
  val method: DisbursementMethod,

  @Schema(description = "Prison NOMIS ID", example = "LEI")
  val prison: String?,

  @Schema(description = "Prisoner number (NOMIS ID)", example = "A1234BC")
  @JsonProperty("prisoner_number")
  val prisonerNumber: String?,

  @Schema(description = "Prisoner full name", example = "John Smith")
  @JsonProperty("prisoner_name")
  val prisonerName: String?,

  @Schema(description = "Recipient first name", example = "Jane")
  @JsonProperty("recipient_first_name")
  val recipientFirstName: String?,

  @Schema(description = "Recipient last name", example = "Doe")
  @JsonProperty("recipient_last_name")
  val recipientLastName: String?,

  @Schema(description = "Computed recipient name", example = "Jane Doe")
  @JsonProperty("recipient_name")
  val recipientName: String?,

  @Schema(description = "Recipient email address", example = "jane@example.com")
  @JsonProperty("recipient_email")
  val recipientEmail: String?,

  @Schema(description = "Address line 1", example = "123 Main Street")
  @JsonProperty("address_line1")
  val addressLine1: String?,

  @Schema(description = "Address line 2", example = "Flat 4")
  @JsonProperty("address_line2")
  val addressLine2: String?,

  @Schema(description = "City", example = "London")
  val city: String?,

  @Schema(description = "Postcode", example = "SW1A 1AA")
  val postcode: String?,

  @Schema(description = "Country", example = "UK")
  val country: String?,

  @Schema(description = "Bank sort code", example = "112233")
  @JsonProperty("sort_code")
  val sortCode: String?,

  @Schema(description = "Bank account number", example = "12345678")
  @JsonProperty("account_number")
  val accountNumber: String?,

  @Schema(description = "Roll number (for building societies)", example = "ROLL001")
  @JsonProperty("roll_number")
  val rollNumber: String?,

  @Schema(description = "Whether recipient is a company", example = "false")
  @JsonProperty("recipient_is_company")
  val recipientIsCompany: Boolean,

  @Schema(description = "Resolution status", example = "PENDING")
  val resolution: DisbursementResolution,

  @Schema(description = "NOMIS transaction ID set during confirm", example = "TXN001")
  @JsonProperty("nomis_transaction_id")
  val nomisTransactionId: String?,

  @Schema(description = "Invoice number generated on confirm", example = "PMD1000042")
  @JsonProperty("invoice_number")
  val invoiceNumber: String?,

  @Schema(description = "Timestamp when record was created", example = "2024-03-15T10:30:00")
  val created: LocalDateTime?,

  @Schema(description = "Timestamp when record was last modified", example = "2024-03-15T10:30:00")
  val modified: LocalDateTime?,

  @Schema(description = "Comments on this disbursement")
  val comments: List<DisbursementCommentDto>,
) {
  companion object {
    fun from(disbursement: Disbursement): DisbursementDto = DisbursementDto(
      id = disbursement.id,
      amount = disbursement.amount,
      method = disbursement.method,
      prison = disbursement.prison,
      prisonerNumber = disbursement.prisonerNumber,
      prisonerName = disbursement.prisonerName,
      recipientFirstName = disbursement.recipientFirstName,
      recipientLastName = disbursement.recipientLastName,
      recipientName = disbursement.recipientName,
      recipientEmail = disbursement.recipientEmail,
      addressLine1 = disbursement.addressLine1,
      addressLine2 = disbursement.addressLine2,
      city = disbursement.city,
      postcode = disbursement.postcode,
      country = disbursement.country,
      sortCode = disbursement.sortCode,
      accountNumber = disbursement.accountNumber,
      rollNumber = disbursement.rollNumber,
      recipientIsCompany = disbursement.recipientIsCompany,
      resolution = disbursement.resolution,
      nomisTransactionId = disbursement.nomisTransactionId,
      invoiceNumber = disbursement.invoiceNumber,
      created = disbursement.created,
      modified = disbursement.modified,
      comments = disbursement.comments.map { DisbursementCommentDto.from(it) },
    )
  }
}
