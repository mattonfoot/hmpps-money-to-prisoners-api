package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(name = "BankTransferRecipientDetails", description = "Bank transfer details for a recipient")
data class BankTransferRecipientDetails(
  @JsonProperty("recipient_sort_code")
  val recipientSortCode: String?,
  @JsonProperty("recipient_account_number")
  val recipientAccountNumber: String?,
  @JsonProperty("recipient_roll_number")
  val recipientRollNumber: String?,
)

@Schema(description = "A recipient profile aggregating disbursements to one bank account")
data class RecipientProfile(
  val id: Long?,
  @JsonProperty("sort_code")
  val sortCode: String?,
  @JsonProperty("account_number")
  val accountNumber: String?,
  @JsonProperty("bank_transfer_details")
  val bankTransferDetails: List<BankTransferRecipientDetails>,
  @JsonProperty("monitoring_users")
  val monitoringUsers: List<String>,
  val monitoring: Boolean?,
  val created: OffsetDateTime?,
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(
      profile: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.RecipientProfile,
      currentUsername: String? = null,
      details: List<uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityBanktransferrecipientdetail> = emptyList(),
      isMonitoredByCurrentUser: Boolean? = null,
    ): RecipientProfile {
      val firstAccount = details.firstNotNullOfOrNull { it.recipientBankAccount }
      return RecipientProfile(
        id = profile.id,
        // Django: flat sort_code/account_number on the response come from the
        // first associated detail's bank account.
        sortCode = firstAccount?.sortCode,
        accountNumber = firstAccount?.accountNumber,
        bankTransferDetails = details.mapNotNull { it.recipientBankAccount }.map { acc ->
          BankTransferRecipientDetails(
            recipientSortCode = acc.sortCode,
            recipientAccountNumber = acc.accountNumber,
            recipientRollNumber = acc.rollNumber.ifEmpty { null },
          )
        },
        monitoringUsers = emptyList(),
        monitoring = isMonitoredByCurrentUser,
        created = profile.created,
        modified = profile.modified,
      )
    }
  }
}
