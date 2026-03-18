package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.BillingAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction

@DisplayName("SecurityCreditDto")
class SecurityCreditDtoTest {

  private fun createCredit(
    id: Long? = 1L,
    amount: Long = 1000,
    prison: String? = "LEI",
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    source: CreditSource = CreditSource.BANK_TRANSFER,
  ): Credit {
    val credit = Credit(
      id = id,
      amount = amount,
      prison = prison,
      resolution = resolution,
      blocked = blocked,
    )
    credit.source = source
    credit.onCreate()
    return credit
  }

  @Nested
  @DisplayName("CRD-106: Security serializer adds bank/card details")
  inner class BankCardDetails {

    @Test
    fun `includes sort_code from transaction`() {
      val credit = createCredit()
      val transaction = Transaction(amount = 1000)
      transaction.senderSortCode = "112233"
      transaction.credit = credit
      credit.transaction = transaction

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.senderSortCode).isEqualTo("112233")
    }

    @Test
    fun `includes account_number from transaction`() {
      val credit = createCredit()
      val transaction = Transaction(amount = 1000)
      transaction.senderAccountNumber = "12345678"
      transaction.credit = credit
      credit.transaction = transaction

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.senderAccountNumber).isEqualTo("12345678")
    }

    @Test
    fun `includes roll_number from transaction`() {
      val credit = createCredit()
      val transaction = Transaction(amount = 1000)
      transaction.senderRollNumber = "ROLL001"
      transaction.credit = credit
      credit.transaction = transaction

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.senderRollNumber).isEqualTo("ROLL001")
    }

    @Test
    fun `includes card_number_first_digits from payment`() {
      val credit = createCredit()
      val payment = Payment()
      payment.cardNumberFirstDigits = "411111"
      payment.credit = credit
      credit.payment = payment

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.cardNumberFirstDigits).isEqualTo("411111")
    }

    @Test
    fun `includes card_number_last_digits from payment`() {
      val credit = createCredit()
      val payment = Payment()
      payment.cardNumberLastDigits = "1234"
      payment.credit = credit
      credit.payment = payment

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.cardNumberLastDigits).isEqualTo("1234")
    }

    @Test
    fun `includes card_expiry_date from payment`() {
      val credit = createCredit()
      val payment = Payment()
      payment.cardExpiryDate = "12/25"
      payment.credit = credit
      credit.payment = payment

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.cardExpiryDate).isEqualTo("12/25")
    }

    @Test
    fun `includes ip_address from payment`() {
      val credit = createCredit()
      val payment = Payment()
      payment.ipAddress = "192.168.1.1"
      payment.credit = credit
      credit.payment = payment

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.senderIpAddress).isEqualTo("192.168.1.1")
    }

    @Test
    fun `includes billing_address from payment`() {
      val credit = createCredit()
      val billingAddress = BillingAddress(
        line1 = "10 Downing Street",
        line2 = null,
        city = "London",
        country = "GB",
        postcode = "SW1A 2AA",
      )
      billingAddress.onCreate()
      val payment = Payment()
      payment.billingAddress = billingAddress
      payment.credit = credit
      credit.payment = payment

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.billingAddress).isNotNull
      assertThat(dto.billingAddress!!.line1).isEqualTo("10 Downing Street")
      assertThat(dto.billingAddress!!.city).isEqualTo("London")
      assertThat(dto.billingAddress!!.postcode).isEqualTo("SW1A 2AA")
    }

    @Test
    fun `billing_address is null when no payment`() {
      val credit = createCredit()
      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.billingAddress).isNull()
    }

    @Test
    fun `billing_address is null when payment has no billing address`() {
      val credit = createCredit()
      val payment = Payment()
      payment.credit = credit
      credit.payment = payment

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.billingAddress).isNull()
    }

    @Test
    fun `bank fields are null when no transaction`() {
      val credit = createCredit()
      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.senderSortCode).isNull()
      assertThat(dto.senderAccountNumber).isNull()
      assertThat(dto.senderRollNumber).isNull()
    }

    @Test
    fun `card fields are null when no payment`() {
      val credit = createCredit()
      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.cardNumberFirstDigits).isNull()
      assertThat(dto.cardNumberLastDigits).isNull()
      assertThat(dto.cardExpiryDate).isNull()
      assertThat(dto.senderIpAddress).isNull()
    }
  }

  @Nested
  @DisplayName("CRD-107: Security serializer adds profile PKs")
  inner class ProfilePKs {

    @Test
    fun `includes sender_profile ID`() {
      val credit = createCredit()
      val senderProfile = SenderProfile(id = 42L)
      senderProfile.credits.add(credit)

      val dto = SecurityCreditDto.from(credit, senderProfileId = 42L)
      assertThat(dto.senderProfile).isEqualTo(42L)
    }

    @Test
    fun `sender_profile is null when not provided`() {
      val credit = createCredit()
      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.senderProfile).isNull()
    }

    @Test
    fun `includes prisoner_profile ID`() {
      val credit = createCredit()
      val dto = SecurityCreditDto.from(credit, prisonerProfileId = 99L)
      assertThat(dto.prisonerProfile).isEqualTo(99L)
    }

    @Test
    fun `prisoner_profile is null when not provided`() {
      val credit = createCredit()
      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.prisonerProfile).isNull()
    }
  }

  @Nested
  @DisplayName("Inherits base CreditDto fields")
  inner class InheritsBaseFields {

    @Test
    fun `includes all base credit fields`() {
      val credit = createCredit(
        id = 42L,
        amount = 5000,
        prison = "LEI",
        resolution = CreditResolution.CREDITED,
      )
      credit.owner = "clerk1"

      val dto = SecurityCreditDto.from(credit)
      assertThat(dto.id).isEqualTo(42L)
      assertThat(dto.amount).isEqualTo(5000)
      assertThat(dto.prison).isEqualTo("LEI")
      assertThat(dto.owner).isEqualTo("clerk1")
    }
  }
}
