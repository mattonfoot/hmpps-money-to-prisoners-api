package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(hidden = true)
data class PaymentSecurityCheckDto(
  @Schema(description = "Check status", example = "PENDING")
  val status: CheckStatus,

  @Schema(description = "Whether the check has been actioned by a user", example = "false")
  @JsonProperty("user_actioned")
  val userActioned: Boolean,
) {
  companion object {
    fun from(check: SecurityCheck): PaymentSecurityCheckDto = PaymentSecurityCheckDto(
      status = CheckStatus.fromValue(check.status),
      userActioned = check.actionedBy != null,
    )
  }
}

@Schema(description = "A payment record for online prisoner money transfers")
data class Payment(
  @Schema(description = "Payment UUID", example = "550e8400-e29b-41d4-a716-446655440000")
  val uuid: UUID,

  @Schema(description = "Amount in pence", example = "5000")
  val amount: Long,

  @Schema(description = "Service charge in pence", example = "0")
  @JsonProperty("service_charge")
  val serviceCharge: Long,

  @Schema(description = "Payment status: pending, taken, failed, rejected, expired", example = "pending")
  val status: String?,

  @Schema(description = "Processor ID", example = "pay_12345")
  @JsonProperty("processor_id")
  val processorId: String?,

  @Schema(description = "Recipient name", example = "John Prisoner")
  @JsonProperty("recipient_name")
  val recipientName: String?,

  @Schema(description = "Sender email", example = "alice@example.com")
  val email: String?,

  @Schema(description = "Cardholder name", example = "Alice Sender")
  @JsonProperty("cardholder_name")
  val cardholderName: String?,

  @Schema(description = "First 6 digits of card number", example = "411111")
  @JsonProperty("card_number_first_digits")
  val cardNumberFirstDigits: String?,

  @Schema(description = "Last 4 digits of card number", example = "1234")
  @JsonProperty("card_number_last_digits")
  val cardNumberLastDigits: String?,

  @Schema(description = "Card expiry date MM/YY", example = "12/25")
  @JsonProperty("card_expiry_date")
  val cardExpiryDate: String?,

  @Schema(description = "Card brand", example = "Visa")
  @JsonProperty("card_brand")
  val cardBrand: String?,

  @Schema(description = "IP address of the sender", example = "192.168.1.1")
  @JsonProperty("ip_address")
  val ipAddress: String?,

  @Schema(description = "Prisoner number (NOMIS ID)", example = "A1234BC")
  @JsonProperty("prisoner_number")
  val prisonerNumber: String?,

  @Schema(description = "Prisoner date of birth", example = "1990-01-15")
  @JsonProperty("prisoner_dob")
  val prisonerDob: LocalDate?,

  @Schema(description = "Worldpay ID")
  @JsonProperty("worldpay_id")
  val worldpayId: String?,

  @Schema(description = "When the credit was received")
  @JsonProperty("received_at")
  val receivedAt: OffsetDateTime?,

  @Schema(description = "Billing address", nullable = true)
  @JsonProperty("billing_address")
  val billingAddress: BillingAddress?,

  @Schema(description = "Security check attached to this payment, null if none", nullable = true)
  @JsonProperty("security_check")
  val securityCheck: PaymentSecurityCheckDto?,

  @Schema(description = "Timestamp when the payment was created", example = "2024-03-15T10:30:00")
  val created: OffsetDateTime?,

  @Schema(description = "Timestamp when the payment was last modified", example = "2024-03-15T10:30:00")
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(payment: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment): Payment = Payment(
      uuid = payment.uuid ?: java.util.UUID(0L, 0L),
      amount = payment.amount.toLong(),
      serviceCharge = payment.serviceCharge.toLong(),
      status = payment.status,
      processorId = payment.processorId,
      recipientName = payment.recipientName,
      email = payment.email,
      cardholderName = payment.cardholderName,
      cardNumberFirstDigits = payment.cardNumberFirstDigits,
      cardNumberLastDigits = payment.cardNumberLastDigits,
      cardExpiryDate = payment.cardExpiryDate,
      cardBrand = payment.cardBrand,
      ipAddress = payment.ipAddress?.hostAddress,
      prisonerNumber = payment.credit?.prisonerNumber,
      prisonerDob = payment.credit?.prisonerDob,
      worldpayId = payment.worldpayId,
      receivedAt = payment.credit?.receivedAt,
      billingAddress = payment.billingAddress?.let { BillingAddress.from(it) },
      securityCheck = payment.credit?.securityCheck?.let { PaymentSecurityCheckDto.from(it) },
      created = payment.created,
      modified = payment.modified,
    )
  }
}
