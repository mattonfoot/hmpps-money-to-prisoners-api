package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(name = "BankTransferSenderDetails", description = "Bank transfer details for a sender")
data class BankTransferSenderDetails(
  @JsonProperty("sender_name")
  val senderName: String?,
  @JsonProperty("sender_sort_code")
  val senderSortCode: String?,
  @JsonProperty("sender_account_number")
  val senderAccountNumber: String?,
  @JsonProperty("sender_roll_number")
  val senderRollNumber: String?,
)

@Schema(name = "DebitCardSenderDetails", description = "Debit card details for a sender")
data class DebitCardSenderDetails(
  @JsonProperty("card_number_last_digits")
  val cardNumberLastDigits: String?,
  @JsonProperty("card_expiry_date")
  val cardExpiryDate: String?,
  @JsonProperty("cardholder_names")
  val cardholderNames: List<String>,
  @JsonProperty("sender_emails")
  val senderEmails: List<String>,
  val postcode: String? = null,
)

@Schema(description = "A sender profile aggregating credits from one sender")
data class SenderProfile(
  val id: Long?,
  @JsonProperty("credit_count")
  val creditCount: Int,
  @JsonProperty("credit_total")
  val creditTotal: Long,
  @JsonProperty("prisoner_count")
  val prisonerCount: Int,
  @JsonProperty("prison_count")
  val prisonCount: Int,
  val prisons: List<String>,
  @JsonProperty("bank_transfer_details")
  val bankTransferDetails: List<BankTransferSenderDetails>,
  @JsonProperty("debit_card_details")
  val debitCardDetails: List<DebitCardSenderDetails>,
  @JsonProperty("monitoring_users")
  val monitoringUsers: List<String>,
  val monitoring: Boolean?,
  val created: OffsetDateTime?,
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(profile: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile, currentUsername: String? = null): SenderProfile {
      val credits = profile.credits
      val bankDetails = credits.mapNotNull { it.transaction }
        .map { tx ->
          BankTransferSenderDetails(
            senderName = tx.senderName,
            senderSortCode = tx.senderSortCode,
            senderAccountNumber = tx.senderAccountNumber,
            senderRollNumber = tx.senderRollNumber,
          )
        }
        .distinctBy { "${it.senderSortCode}-${it.senderAccountNumber}" }
      val cardDetails = credits.mapNotNull { it.payment }
        .map { pay ->
          DebitCardSenderDetails(
            cardNumberLastDigits = pay.cardNumberLastDigits,
            cardExpiryDate = pay.cardExpiryDate,
            cardholderNames = listOfNotNull(pay.cardholderName),
            senderEmails = listOfNotNull(pay.email),
          )
        }
        .distinctBy { "${it.cardNumberLastDigits}-${it.cardExpiryDate}" }
      return SenderProfile(
        id = profile.id,
        creditCount = credits.size,
        creditTotal = credits.sumOf { it.amount },
        prisonerCount = credits.mapNotNull { it.prisonerNumber }.distinct().size,
        prisonCount = credits.mapNotNull { it.prison }.distinct().size,
        prisons = credits.mapNotNull { it.prison?.nomisId }.distinct(),
        bankTransferDetails = bankDetails,
        debitCardDetails = cardDetails,
        // Sender-profile monitoring users live on the per-detail children in
        // Django (security_debitcardsenderdetails_monitoring_users etc.). Emit
        // empty until per-detail rendering is wired in.
        monitoringUsers = emptyList(),
        monitoring = null,
        created = profile.created,
        modified = profile.modified,
      )
    }
  }
}
