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

  @Test
  fun `bankTransferDetails extracted from credits with transactions`() {
    val profile = SenderProfileEntity()
    val tx = TransactionEntity(
      senderName = "John Smith",
      senderSortCode = "112233",
      senderAccountNumber = "12345678",
      senderRollNumber = "ROLL1",
    )
    val credit = CreditEntity(amount = 1000, resolution = CreditResolution.CREDITED)
    credit.transaction = tx
    profile.credits = mutableSetOf(credit)

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
    val payment = PaymentEntity(amount = 2000, status = "taken")
    payment.cardNumberLastDigits = "4321"
    payment.cardExpiryDate = "12/25"
    payment.cardholderName = "Jane Doe"
    payment.email = "jane@example.com"
    val credit = CreditEntity(amount = 2000, resolution = CreditResolution.CREDITED)
    credit.payment = payment
    profile.credits = mutableSetOf(credit)

    val dto = SenderProfile.from(profile)

    assertThat(dto.debitCardDetails).hasSize(1)
    assertThat(dto.debitCardDetails[0].cardNumberLastDigits).isEqualTo("4321")
    assertThat(dto.debitCardDetails[0].cardExpiryDate).isEqualTo("12/25")
  }

  @Test
  fun `bankTransferDetails deduplicates by sort code and account number`() {
    val profile = SenderProfileEntity()
    val tx1 = TransactionEntity(senderName = "John", senderSortCode = "112233", senderAccountNumber = "12345678")
    val tx2 = TransactionEntity(senderName = "John", senderSortCode = "112233", senderAccountNumber = "12345678")
    val c1 = CreditEntity(amount = 100, resolution = CreditResolution.CREDITED)
    c1.transaction = tx1
    val c2 = CreditEntity(amount = 200, resolution = CreditResolution.CREDITED)
    c2.transaction = tx2
    profile.credits = mutableSetOf(c1, c2)

    val dto = SenderProfile.from(profile)

    assertThat(dto.bankTransferDetails).hasSize(1)
  }
}
