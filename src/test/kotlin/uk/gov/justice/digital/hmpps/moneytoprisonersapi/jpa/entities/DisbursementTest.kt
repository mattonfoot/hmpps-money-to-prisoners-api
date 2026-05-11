package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DisbursementTest {

  private fun createDisbursement(
    amount: Int = 1000,
    method: DisbursementMethod = DisbursementMethod.BANK_TRANSFER,
    prisonNomisId: String? = "LEI",
  ) = Disbursement().apply {
    this.amount = amount
    this.method = method.value
    this.prison = prisonNomisId?.let { nomis -> PrisonPrison().apply { this.nomisId = nomis } }
    this.prisonerNumber = "A1234BC"
    this.prisonerName = "John Smith"
    this.recipientFirstName = "Jane"
    this.recipientLastName = "Doe"
  }

  @Nested
  @DisplayName("DSB-001 to DSB-009: Disbursement Model")
  inner class DisbursementModel {

    @Test
    @DisplayName("DSB-001 - Disbursement holds the required fields after construction")
    fun `should create disbursement with required fields`() {
      val disbursement = createDisbursement(amount = 5000)

      assertThat(disbursement.amount).isEqualTo(5000)
      assertThat(disbursement.method).isEqualTo(DisbursementMethod.BANK_TRANSFER.value)
      assertThat(disbursement.prison?.nomisId).isEqualTo("LEI")
      assertThat(disbursement.prisonerNumber).isEqualTo("A1234BC")
      assertThat(disbursement.prisonerName).isEqualTo("John Smith")
    }

    @Test
    @DisplayName("DSB-002 - Amount stored in pence")
    fun `should store amount in pence`() {
      val disbursement = createDisbursement(amount = 1050)
      assertThat(disbursement.amount).isEqualTo(1050)
    }

    @Test
    @DisplayName("DSB-003 - Method can be BANK_TRANSFER or CHEQUE")
    fun `should support both payment methods`() {
      val bankTransfer = createDisbursement(method = DisbursementMethod.BANK_TRANSFER)
      val cheque = createDisbursement(method = DisbursementMethod.CHEQUE)

      assertThat(bankTransfer.method).isEqualTo(DisbursementMethod.BANK_TRANSFER.value)
      assertThat(cheque.method).isEqualTo(DisbursementMethod.CHEQUE.value)
    }

    @Test
    @DisplayName("DSB-004 - Initial resolution is PENDING")
    fun `should default to PENDING resolution`() {
      val disbursement = createDisbursement()
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PENDING.value)
    }

    @Test
    @DisplayName("DSB-005 - recipientIsCompany defaults to false")
    fun `should default recipientIsCompany to false`() {
      val disbursement = createDisbursement()
      assertThat(disbursement.recipientIsCompany).isFalse()
    }

    @Test
    @DisplayName("DSB-008 - invoiceNumber is settable")
    fun `should allow setting invoice number`() {
      val disbursement = createDisbursement()
      disbursement.invoiceNumber = "PMD${1000000 + 42}"
      assertThat(disbursement.invoiceNumber).isEqualTo("PMD1000042")
    }

    @Test
    @DisplayName("DSB-009 - Optional fields default to null")
    fun `should have sensible defaults for optional fields`() {
      val disbursement = createDisbursement(method = DisbursementMethod.CHEQUE)
      assertThat(disbursement.recipientEmail).isNull()
      assertThat(disbursement.addressLine1).isNull()
      assertThat(disbursement.addressLine2).isNull()
      assertThat(disbursement.city).isNull()
      assertThat(disbursement.postcode).isNull()
      assertThat(disbursement.country).isNull()
      assertThat(disbursement.sortCode).isNull()
      assertThat(disbursement.accountNumber).isNull()
      assertThat(disbursement.rollNumber).isNull()
      assertThat(disbursement.nomisTransactionId).isNull()
      assertThat(disbursement.invoiceNumber).isNull()
    }
  }

  @Nested
  @DisplayName("DSB-010 to DSB-020: Disbursement State Machine")
  inner class StateMachine {

    @Test
    @DisplayName("DSB-010 - PENDING to PRECONFIRMED is valid")
    fun `should allow PENDING to PRECONFIRMED`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.PRECONFIRMED)
      assertThat(d.resolution).isEqualTo(DisbursementResolution.PRECONFIRMED.value)
    }

    @Test
    @DisplayName("DSB-011 - PENDING to REJECTED is valid")
    fun `should allow PENDING to REJECTED`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.REJECTED)
      assertThat(d.resolution).isEqualTo(DisbursementResolution.REJECTED.value)
    }

    @Test
    @DisplayName("DSB-012 - PRECONFIRMED to CONFIRMED is valid")
    fun `should allow PRECONFIRMED to CONFIRMED`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.PRECONFIRMED)
      d.transitionResolution(DisbursementResolution.CONFIRMED)
      assertThat(d.resolution).isEqualTo(DisbursementResolution.CONFIRMED.value)
    }

    @Test
    @DisplayName("DSB-013 - PRECONFIRMED to PENDING is valid")
    fun `should allow PRECONFIRMED to PENDING`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.PRECONFIRMED)
      d.transitionResolution(DisbursementResolution.PENDING)
      assertThat(d.resolution).isEqualTo(DisbursementResolution.PENDING.value)
    }

    @Test
    @DisplayName("DSB-014 - PRECONFIRMED to REJECTED is valid")
    fun `should allow PRECONFIRMED to REJECTED`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.PRECONFIRMED)
      d.transitionResolution(DisbursementResolution.REJECTED)
      assertThat(d.resolution).isEqualTo(DisbursementResolution.REJECTED.value)
    }

    @Test
    @DisplayName("DSB-015 - CONFIRMED to SENT is valid")
    fun `should allow CONFIRMED to SENT`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.PRECONFIRMED)
      d.transitionResolution(DisbursementResolution.CONFIRMED)
      d.transitionResolution(DisbursementResolution.SENT)
      assertThat(d.resolution).isEqualTo(DisbursementResolution.SENT.value)
    }

    @Test
    @DisplayName("DSB-016 - REJECTED to PENDING is valid")
    fun `should allow REJECTED to PENDING`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.REJECTED)
      d.transitionResolution(DisbursementResolution.PENDING)
      assertThat(d.resolution).isEqualTo(DisbursementResolution.PENDING.value)
    }

    @Test
    @DisplayName("DSB-017 - SENT is terminal - no further transitions")
    fun `should not allow transition from SENT`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.PRECONFIRMED)
      d.transitionResolution(DisbursementResolution.CONFIRMED)
      d.transitionResolution(DisbursementResolution.SENT)

      assertThatThrownBy { d.transitionResolution(DisbursementResolution.PENDING) }
        .isInstanceOf(InvalidDisbursementStateException::class.java)
    }

    @Test
    @DisplayName("DSB-018 - Invalid transition throws exception")
    fun `should throw on invalid transition`() {
      val d = createDisbursement()
      assertThatThrownBy { d.transitionResolution(DisbursementResolution.CONFIRMED) }
        .isInstanceOf(InvalidDisbursementStateException::class.java)
    }

    @Test
    @DisplayName("DSB-019 - Idempotent: already in target state is no-op")
    fun `should be idempotent when already in target state`() {
      val d = createDisbursement()
      d.transitionResolution(DisbursementResolution.PENDING)
      assertThat(d.resolution).isEqualTo(DisbursementResolution.PENDING.value)
    }

    @Test
    @DisplayName("DSB-020 - PENDING to SENT is invalid")
    fun `should not allow direct PENDING to SENT`() {
      val d = createDisbursement()
      assertThatThrownBy { d.transitionResolution(DisbursementResolution.SENT) }
        .isInstanceOf(InvalidDisbursementStateException::class.java)
    }
  }
}
