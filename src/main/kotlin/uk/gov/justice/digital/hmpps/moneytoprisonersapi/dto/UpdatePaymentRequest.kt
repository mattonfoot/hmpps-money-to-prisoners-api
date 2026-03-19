package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Request body for partially updating a payment (PATCH)")
data class UpdatePaymentRequest(
  @Schema(description = "New status: taken, failed, rejected, or expired", example = "taken")
  val status: String? = null,

  @Schema(description = "Cardholder name", example = "John Doe")
  @JsonProperty("cardholder_name")
  val cardholderName: String? = null,

  @Schema(description = "First 6 digits of card number", example = "411111")
  @JsonProperty("card_number_first_digits")
  val cardNumberFirstDigits: String? = null,

  @Schema(description = "Last 4 digits of card number", example = "1234")
  @JsonProperty("card_number_last_digits")
  val cardNumberLastDigits: String? = null,

  @Schema(description = "Card expiry date MM/YY", example = "12/25")
  @JsonProperty("card_expiry_date")
  val cardExpiryDate: String? = null,

  @Schema(description = "Card brand", example = "Visa")
  @JsonProperty("card_brand")
  val cardBrand: String? = null,

  @Schema(description = "IP address of the sender", example = "192.168.1.1")
  @JsonProperty("ip_address")
  val ipAddress: String? = null,

  @Schema(description = "When the payment was received (ISO datetime). Defaults to now when status=taken.", example = "2024-03-15T10:30:00")
  @JsonProperty("received_at")
  val receivedAt: LocalDateTime? = null,

  @Schema(description = "Billing address details")
  @JsonProperty("billing_address")
  val billingAddress: BillingAddressUpdateDto? = null,
)
