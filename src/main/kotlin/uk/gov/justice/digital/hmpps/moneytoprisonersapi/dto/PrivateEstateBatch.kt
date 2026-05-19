package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A private estate batch grouping credits for a private prison on a specific date")
data class PrivateEstateBatch(
  @Schema(description = "Date of the batch", example = "2024-03-15")
  val date: LocalDate,
  @Schema(description = "Prison NOMIS ID", example = "PRV")
  val prison: String,
  @Schema(description = "Total amount in pence for all credits in this batch", example = "50000")
  @JsonProperty("total_amount")
  val totalAmount: Long,
  @Schema(description = "Prison bank account details")
  @JsonProperty("bank_account")
  val bankAccount: PrisonBankAccount?,
  @Schema(description = "Ordered remittance email addresses for the prison")
  @JsonProperty("remittance_emails")
  val remittanceEmails: List<String>,
) {
  companion object {
    fun from(
      batch: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch,
      bankAccount: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrisonbankaccount?,
      remittanceEmails: List<String>,
    ): PrivateEstateBatch {
      val prisonId = batch.prison?.nomisId ?: ""
      return PrivateEstateBatch(
        date = batch.date,
        prison = prisonId,
        totalAmount = batch.credits.sumOf { it.amount },
        bankAccount = bankAccount?.let { PrisonBankAccount.from(it) },
        remittanceEmails = remittanceEmails,
      )
    }
  }
}

@Schema(description = "Prison bank account details")
data class PrisonBankAccount(
  val id: Long?,
  @JsonProperty("address_line1")
  val addressLine1: String,
  @JsonProperty("address_line2")
  val addressLine2: String,
  val city: String,
  val postcode: String,
  @JsonProperty("sort_code")
  val sortCode: String,
  @JsonProperty("account_number")
  val accountNumber: String,
  val prison: String,
) {
  companion object {
    fun from(account: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrisonbankaccount) = PrisonBankAccount(
      id = account.id,
      addressLine1 = account.addressLine1,
      addressLine2 = account.addressLine2,
      city = account.city,
      postcode = account.postcode,
      sortCode = account.sortCode,
      accountNumber = account.accountNumber,
      prison = account.prison?.nomisId ?: "",
    )
  }
}
