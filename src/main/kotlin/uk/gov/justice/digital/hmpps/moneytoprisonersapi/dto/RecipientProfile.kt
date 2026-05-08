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
    fun from(profile: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.RecipientProfile, currentUsername: String? = null): RecipientProfile = RecipientProfile(
      id = profile.id,
      // Django stores sortCode/accountNumber on security_banktransferrecipientdetail
      // children, not on the parent profile. These flat top-level fields are
      // populated as null until per-detail rendering is wired in.
      sortCode = null,
      accountNumber = null,
      bankTransferDetails = emptyList(),
      // monitoringUsers also lives on per-detail children; emit empty for now.
      monitoringUsers = emptyList(),
      monitoring = null,
      created = profile.created,
      modified = profile.modified,
    )
  }
}
