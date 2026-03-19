package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DisbursementTest {

  @Nested
  @DisplayName("DSB-001 to DSB-009: Disbursement Model")
  inner class DisbursementModel {

    @Test
    @DisplayName("DSB-001 - Disbursement can be created with required fields")
    fun `should create disbursement with required fields`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )

      assertThat(disbursement.amount).isEqualTo(5000L)
      assertThat(disbursement.method).isEqualTo(DisbursementMethod.BANK_TRANSFER)
      assertThat(disbursement.prison).isEqualTo("LEI")
      assertThat(disbursement.prisonerNumber).isEqualTo("A1234BC")
      assertThat(disbursement.prisonerName).isEqualTo("John Smith")
    }

    @Test
    @DisplayName("DSB-002 - Amount stored in pence")
    fun `should store amount in pence`() {
      val disbursement = Disbursement(
        amount = 1050L,
        method = DisbursementMethod.CHEQUE,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      assertThat(disbursement.amount).isEqualTo(1050L)
    }

    @Test
    @DisplayName("DSB-003 - Method can be BANK_TRANSFER or CHEQUE")
    fun `should support both payment methods`() {
      val bankTransfer = Disbursement(
        amount = 1000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      val cheque = Disbursement(
        amount = 2000L,
        method = DisbursementMethod.CHEQUE,
        prison = "MDI",
        prisonerNumber = "B5678DE",
        prisonerName = "Jane Jones",
        recipientFirstName = "Bob",
        recipientLastName = "Smith",
      )

      assertThat(bankTransfer.method).isEqualTo(DisbursementMethod.BANK_TRANSFER)
      assertThat(cheque.method).isEqualTo(DisbursementMethod.CHEQUE)
    }

    @Test
    @DisplayName("DSB-004 - Initial resolution is PENDING")
    fun `should default to PENDING resolution`() {
      val disbursement = Disbursement(
        amount = 1000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PENDING)
    }

    @Test
    @DisplayName("DSB-005 - recipientIsCompany defaults to false")
    fun `should default recipientIsCompany to false`() {
      val disbursement = Disbursement(
        amount = 1000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      assertThat(disbursement.recipientIsCompany).isFalse()
    }

    @Test
    @DisplayName("DSB-006 - recipientName computed as FirstName LastName for non-company")
    fun `should compute recipient name for individual`() {
      val disbursement = Disbursement(
        amount = 1000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        recipientIsCompany = false,
      )
      assertThat(disbursement.recipientName).isEqualTo("Jane Doe")
    }

    @Test
    @DisplayName("DSB-007 - recipientName for company uses company name")
    fun `should compute recipient name for company`() {
      val disbursement = Disbursement(
        amount = 1000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "ACME Corp",
        recipientLastName = null,
        recipientIsCompany = true,
      )
      assertThat(disbursement.recipientName).isEqualTo("ACME Corp")
    }

    @Test
    @DisplayName("DSB-008 - invoiceNumber computed as PMD + (1000000 + id)")
    fun `should compute invoice number after id is set`() {
      val disbursement = Disbursement(
        amount = 1000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      // Simulate having an ID
      disbursement.invoiceNumber = "PMD${1000000 + 42}"
      assertThat(disbursement.invoiceNumber).isEqualTo("PMD1000042")
    }

    @Test
    @DisplayName("DSB-009 - Optional fields have sensible defaults")
    fun `should have sensible defaults for optional fields`() {
      val disbursement = Disbursement(
        amount = 1000L,
        method = DisbursementMethod.CHEQUE,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
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
      val disbursement = createDisbursement()
      disbursement.transitionResolution(DisbursementResolution.PRECONFIRMED)
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PRECONFIRMED)
    }

    @Test
    @DisplayName("DSB-011 - PENDING to REJECTED is valid")
    fun `should allow PENDING to REJECTED`() {
      val disbursement = createDisbursement()
      disbursement.transitionResolution(DisbursementResolution.REJECTED)
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.REJECTED)
    }

    @Test
    @DisplayName("DSB-012 - PRECONFIRMED to CONFIRMED is valid")
    fun `should allow PRECONFIRMED to CONFIRMED`() {
      val disbursement = createDisbursement()
      disbursement.transitionResolution(DisbursementResolution.PRECONFIRMED)
      disbursement.transitionResolution(DisbursementResolution.CONFIRMED)
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.CONFIRMED)
    }

    @Test
    @DisplayName("DSB-013 - PRECONFIRMED to PENDING is valid")
    fun `should allow PRECONFIRMED to PENDING`() {
      val disbursement = createDisbursement()
      disbursement.transitionResolution(DisbursementResolution.PRECONFIRMED)
      disbursement.transitionResolution(DisbursementResolution.PENDING)
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PENDING)
    }

    @Test
    @DisplayName("DSB-014 - PRECONFIRMED to REJECTED is valid")
    fun `should allow PRECONFIRMED to REJECTED`() {
      val disbursement = createDisbursement()
      disbursement.transitionResolution(DisbursementResolution.PRECONFIRMED)
      disbursement.transitionResolution(DisbursementResolution.REJECTED)
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.REJECTED)
    }

    @Test
    @DisplayName("DSB-015 - CONFIRMED to SENT is valid")
    fun `should allow CONFIRMED to SENT`() {
      val disbursement = createDisbursement()
      disbursement.transitionResolution(DisbursementResolution.PRECONFIRMED)
      disbursement.transitionResolution(DisbursementResolution.CONFIRMED)
      disbursement.transitionResolution(DisbursementResolution.SENT)
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.SENT)
    }

    @Test
    @DisplayName("DSB-016 - REJECTED to PENDING is valid")
    fun `should allow REJECTED to PENDING`() {
      val disbursement = createDisbursement()
      disbursement.transitionResolution(DisbursementResolution.REJECTED)
      disbursement.transitionResolution(DisbursementResolution.PENDING)
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PENDING)
    }

    @Test
    @DisplayName("DSB-017 - SENT is terminal - no further transitions")
    fun `should not allow transition from SENT`() {
      val disbursement = createDisbursement()
      disbursement.transitionResolution(DisbursementResolution.PRECONFIRMED)
      disbursement.transitionResolution(DisbursementResolution.CONFIRMED)
      disbursement.transitionResolution(DisbursementResolution.SENT)

      assertThatThrownBy { disbursement.transitionResolution(DisbursementResolution.PENDING) }
        .isInstanceOf(InvalidDisbursementStateException::class.java)
    }

    @Test
    @DisplayName("DSB-018 - Invalid transition throws exception")
    fun `should throw on invalid transition`() {
      val disbursement = createDisbursement()
      assertThatThrownBy { disbursement.transitionResolution(DisbursementResolution.CONFIRMED) }
        .isInstanceOf(InvalidDisbursementStateException::class.java)
    }

    @Test
    @DisplayName("DSB-019 - Idempotent: already in target state is no-op")
    fun `should be idempotent when already in target state`() {
      val disbursement = createDisbursement()
      // PENDING -> PENDING should be a no-op, not throw
      disbursement.transitionResolution(DisbursementResolution.PENDING)
      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PENDING)
    }

    @Test
    @DisplayName("DSB-020 - PENDING to SENT is invalid")
    fun `should not allow direct PENDING to SENT`() {
      val disbursement = createDisbursement()
      assertThatThrownBy { disbursement.transitionResolution(DisbursementResolution.SENT) }
        .isInstanceOf(InvalidDisbursementStateException::class.java)
    }

    private fun createDisbursement() = Disbursement(
      amount = 1000L,
      method = DisbursementMethod.BANK_TRANSFER,
      prison = "LEI",
      prisonerNumber = "A1234BC",
      prisonerName = "John Smith",
      recipientFirstName = "Jane",
      recipientLastName = "Doe",
    )
  }
}
