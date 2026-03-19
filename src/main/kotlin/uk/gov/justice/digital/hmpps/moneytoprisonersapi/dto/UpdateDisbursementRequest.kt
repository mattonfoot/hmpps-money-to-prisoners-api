package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod

@Schema(description = "Request body for partially updating a disbursement")
data class UpdateDisbursementRequest(
  @Schema(description = "Amount in pence", example = "5000")
  val amount: Long? = null,

  @Schema(description = "Payment method", example = "BANK_TRANSFER")
  val method: DisbursementMethod? = null,

  @Schema(description = "Prison NOMIS ID", example = "LEI")
  val prison: String? = null,

  @Schema(description = "Prisoner number (NOMIS ID)", example = "A1234BC")
  @JsonProperty("prisoner_number")
  val prisonerNumber: String? = null,

  @Schema(description = "Prisoner full name", example = "John Smith")
  @JsonProperty("prisoner_name")
  val prisonerName: String? = null,

  @Schema(description = "Recipient first name", example = "Jane")
  @JsonProperty("recipient_first_name")
  val recipientFirstName: String? = null,

  @Schema(description = "Recipient last name", example = "Doe")
  @JsonProperty("recipient_last_name")
  val recipientLastName: String? = null,

  @Schema(description = "Recipient email address", example = "jane@example.com")
  @JsonProperty("recipient_email")
  val recipientEmail: String? = null,

  @Schema(description = "Address line 1", example = "123 Main Street")
  @JsonProperty("address_line1")
  val addressLine1: String? = null,

  @Schema(description = "Address line 2", example = "Flat 4")
  @JsonProperty("address_line2")
  val addressLine2: String? = null,

  @Schema(description = "City", example = "London")
  val city: String? = null,

  @Schema(description = "Postcode", example = "SW1A 1AA")
  val postcode: String? = null,

  @Schema(description = "Country", example = "UK")
  val country: String? = null,

  @Schema(description = "Bank sort code", example = "112233")
  @JsonProperty("sort_code")
  val sortCode: String? = null,

  @Schema(description = "Bank account number", example = "12345678")
  @JsonProperty("account_number")
  val accountNumber: String? = null,

  @Schema(description = "Roll number (for building societies)", example = "ROLL001")
  @JsonProperty("roll_number")
  val rollNumber: String? = null,

  @Schema(description = "Whether recipient is a company", example = "false")
  @JsonProperty("recipient_is_company")
  val recipientIsCompany: Boolean? = null,
)
