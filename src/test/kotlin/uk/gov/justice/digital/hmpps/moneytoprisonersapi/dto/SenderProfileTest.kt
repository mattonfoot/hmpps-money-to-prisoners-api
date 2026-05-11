package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit as CreditEntity
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment as PaymentEntity
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile as SenderProfileEntity
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction as TransactionEntity

@DisplayName("SenderProfile")
class SenderProfileTest {

  private fun credit(amount: Long = 1000) = CreditEntity().apply {
    this.amount = amount
    this.resolution = CreditResolution.CREDITED.value
  }

  @Test
  fun `bankTransferDetails extracted from credits with transactions`() {
    val profile = SenderProfileEntity()
    val tx = TransactionEntity().apply {
      senderName = "John Smith"
      senderSortCode = "112233"
      senderAccountNumber = "12345678"
      senderRollNumber = "ROLL1"
    }
    val credit = credit().apply { transaction = tx }
    profile.credits = mutableListOf(credit)

    val dto = SenderProfile.from(profile)

    assertThat(dto.bankTransferDetails).hasSize(1)
    assertThat(dto.bankTransferDetails[0].senderName).isEqualTo("John Smith")
    assertThat(dto.bankTransferDetails[0].senderSortCode).isEqualTo("112233")
    assertThat(dto.bankTransferDetails[0].senderAccountNumber).isEqualTo("12345678")
    assertThat(dto.bankTransferDetails[0].senderRollNumber).isEqualTo("ROLL1")
  }

  @Test
  fun `debitCardDetails extracted from credits with payments`() {
    val profile = SenderProfileEntity()
    val payment = PaymentEntity().apply {
      amount = 2000
      status = "taken"
      cardNumberLastDigits = "4321"
      cardExpiryDate = "12/25"
      cardholderName = "Jane Doe"
      email = "jane@example.com"
    }
    val credit = credit(2000).apply { this.payment = payment }
    profile.credits = mutableListOf(credit)

    val dto = SenderProfile.from(profile)

    assertThat(dto.debitCardDetails).hasSize(1)
    assertThat(dto.debitCardDetails[0].cardNumberLastDigits).isEqualTo("4321")
    assertThat(dto.debitCardDetails[0].cardExpiryDate).isEqualTo("12/25")
  }

  @Test
  fun `bankTransferDetails deduplicates by sort code and account number`() {
    val profile = SenderProfileEntity()
    val tx1 = TransactionEntity().apply {
      senderName = "John"
      senderSortCode = "112233"
      senderAccountNumber = "12345678"
    }
    val tx2 = TransactionEntity().apply {
      senderName = "John"
      senderSortCode = "112233"
      senderAccountNumber = "12345678"
    }
    val c1 = credit(100).apply { transaction = tx1 }
    val c2 = credit(200).apply { transaction = tx2 }
    profile.credits = mutableListOf(c1, c2)

    val dto = SenderProfile.from(profile)

    assertThat(dto.bankTransferDetails).hasSize(1)
  }
}
