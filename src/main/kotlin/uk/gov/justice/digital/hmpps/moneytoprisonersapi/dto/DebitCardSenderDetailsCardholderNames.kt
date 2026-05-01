package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Alternative debit card sender details schema that includes a `sender` reference,
 * used in `CheckAutoAcceptRule.debit_card_sender_details`.
 * Mirrors Python's `DebitCardSenderDetailsCardholderNames` schema.
 */
@Schema(name = "DebitCardSenderDetailsCardholderNames", description = "Debit card sender details with embedded sender reference")
data class DebitCardSenderDetailsCardholderNames(
  @JsonProperty("card_number_last_digits")
  val cardNumberLastDigits: String? = null,
  @JsonProperty("card_expiry_date")
  val cardExpiryDate: String? = null,
  @JsonProperty("cardholder_names")
  val cardholderNames: List<String> = emptyList(),
  @JsonProperty("sender_emails")
  val senderEmails: List<String> = emptyList(),
  val postcode: String? = null,
  val sender: Long? = null,
)
