package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import java.time.LocalDateTime

@Schema(description = "A sender profile aggregating credits from one sender")
data class SenderProfileDto(
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
  val bankTransferDetails: List<Map<String, Any?>>,
  @JsonProperty("debit_card_details")
  val debitCardDetails: List<Map<String, Any?>>,
  @JsonProperty("monitoring_users")
  val monitoringUsers: List<String>,
  val monitoring: Boolean?,
  val created: LocalDateTime?,
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(profile: SenderProfile, currentUsername: String? = null): SenderProfileDto {
      val credits = profile.credits
      val bankDetails = credits.mapNotNull { it.transaction }
        .map { tx ->
          mapOf<String, Any?>(
            "sender_name" to tx.senderName,
            "sender_sort_code" to tx.senderSortCode,
            "sender_account_number" to tx.senderAccountNumber,
            "sender_roll_number" to tx.senderRollNumber,
          )
        }
        .distinctBy { "${it["sender_sort_code"]}-${it["sender_account_number"]}" }
      val cardDetails = credits.mapNotNull { it.payment }
        .map { pay ->
          mapOf<String, Any?>(
            "card_number_last_digits" to pay.cardNumberLastDigits,
            "card_expiry_date" to pay.cardExpiryDate,
            "cardholder_names" to listOfNotNull(pay.cardholderName),
            "sender_emails" to listOfNotNull(pay.email),
          )
        }
        .distinctBy { "${it["card_number_last_digits"]}-${it["card_expiry_date"]}" }
      return SenderProfileDto(
        id = profile.id,
        creditCount = credits.size,
        creditTotal = credits.sumOf { it.amount },
        prisonerCount = credits.mapNotNull { it.prisonerNumber }.distinct().size,
        prisonCount = credits.mapNotNull { it.prison }.distinct().size,
        prisons = credits.mapNotNull { it.prison }.distinct(),
        bankTransferDetails = bankDetails,
        debitCardDetails = cardDetails,
        monitoringUsers = profile.monitoringUsers.toList(),
        monitoring = if (currentUsername != null) profile.monitoringUsers.contains(currentUsername) else null,
        created = profile.created,
        modified = profile.modified,
      )
    }
  }
}
