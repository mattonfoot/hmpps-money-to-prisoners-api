package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Credit Model")
class CreditTest {

  private fun createCredit(
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prisonerName: String? = "John Smith",
    prisonerDob: LocalDate? = LocalDate.of(1990, 1, 15),
    prison: String? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    reviewed: Boolean = false,
    reconciled: Boolean = false,
    receivedAt: LocalDateTime? = null,
    owner: String? = null,
    nomisTransactionId: String? = null,
    incompleteSenderInfo: Boolean = false,
  ): Credit = Credit(
    amount = amount,
    prisonerNumber = prisonerNumber,
    prisonerName = prisonerName,
    prisonerDob = prisonerDob,
    prison = prison,
    resolution = resolution,
    blocked = blocked,
    reviewed = reviewed,
    reconciled = reconciled,
    receivedAt = receivedAt,
    owner = owner,
    nomisTransactionId = nomisTransactionId,
    incompleteSenderInfo = incompleteSenderInfo,
  )

  @Nested
  @DisplayName("CRD-001: Credit has amount in pence")
  inner class AmountField {

    @Test
    fun `amount stores value as positive long`() {
      val credit = createCredit(amount = 12345)
      assertEquals(12345L, credit.amount)
    }

    @Test
    fun `amount handles large values`() {
      val credit = createCredit(amount = 999999999L)
      assertEquals(999999999L, credit.amount)
    }

    @Test
    fun `amount handles minimum positive value`() {
      val credit = createCredit(amount = 1)
      assertEquals(1L, credit.amount)
    }
  }

  @Nested
  @DisplayName("CRD-002: Credit links to prisoner")
  inner class PrisonerFields {

    @Test
    fun `prisoner_number is stored`() {
      val credit = createCredit(prisonerNumber = "A1234BC")
      assertEquals("A1234BC", credit.prisonerNumber)
    }

    @Test
    fun `prisoner_name is stored`() {
      val credit = createCredit(prisonerName = "John Smith")
      assertEquals("John Smith", credit.prisonerName)
    }

    @Test
    fun `prisoner_dob is stored as LocalDate`() {
      val dob = LocalDate.of(1990, 1, 15)
      val credit = createCredit(prisonerDob = dob)
      assertEquals(dob, credit.prisonerDob)
    }

    @Test
    fun `prisoner fields are nullable`() {
      val credit = createCredit(
        prisonerNumber = null,
        prisonerName = null,
        prisonerDob = null,
      )
      assertNull(credit.prisonerNumber)
      assertNull(credit.prisonerName)
      assertNull(credit.prisonerDob)
    }
  }

  @Nested
  @DisplayName("CRD-003: Credit links to prison")
  inner class PrisonField {

    @Test
    fun `prison is nullable`() {
      val credit = createCredit(prison = null)
      assertNull(credit.prison)
    }

    @Test
    fun `prison stores NOMIS ID when set`() {
      val credit = createCredit(prison = "LEI")
      assertEquals("LEI", credit.prison)
    }
  }

  @Nested
  @DisplayName("CRD-005: Credit has resolution status")
  inner class ResolutionField {

    @Test
    fun `default resolution is pending`() {
      val credit = createCredit()
      assertEquals(CreditResolution.PENDING, credit.resolution)
    }

    @Test
    fun `resolution can be set to each valid value`() {
      CreditResolution.entries.forEach { resolution ->
        val credit = createCredit(resolution = resolution)
        assertEquals(resolution, credit.resolution)
      }
    }
  }

  @Nested
  @DisplayName("CRD-006: Resolution transitions enforced")
  inner class ResolutionTransitions {

    @Test
    fun `pending can transition to credited`() {
      val credit = createCredit(resolution = CreditResolution.PENDING, prison = "LEI")
      credit.resolution = CreditResolution.CREDITED
      assertEquals(CreditResolution.CREDITED, credit.resolution)
    }

    @Test
    fun `pending can transition to refunded`() {
      val credit = createCredit(resolution = CreditResolution.PENDING)
      credit.resolution = CreditResolution.REFUNDED
      assertEquals(CreditResolution.REFUNDED, credit.resolution)
    }

    @Test
    fun `pending can transition to manual`() {
      val credit = createCredit(resolution = CreditResolution.PENDING)
      credit.resolution = CreditResolution.MANUAL
      assertEquals(CreditResolution.MANUAL, credit.resolution)
    }

    @Test
    fun `manual can transition to credited`() {
      val credit = createCredit(resolution = CreditResolution.MANUAL, prison = "LEI")
      credit.resolution = CreditResolution.CREDITED
      assertEquals(CreditResolution.CREDITED, credit.resolution)
    }

    @Test
    fun `credited is terminal - cannot transition`() {
      val credit = createCredit(resolution = CreditResolution.CREDITED)
      assertThrows<InvalidCreditStateException> {
        credit.transitionResolution(CreditResolution.PENDING)
      }
    }

    @Test
    fun `refunded is terminal - cannot transition`() {
      val credit = createCredit(resolution = CreditResolution.REFUNDED)
      assertThrows<InvalidCreditStateException> {
        credit.transitionResolution(CreditResolution.PENDING)
      }
    }

    @Test
    fun `failed is terminal - cannot transition`() {
      val credit = createCredit(resolution = CreditResolution.FAILED)
      assertThrows<InvalidCreditStateException> {
        credit.transitionResolution(CreditResolution.PENDING)
      }
    }

    @Test
    fun `initial can transition to pending`() {
      val credit = createCredit(resolution = CreditResolution.INITIAL)
      credit.transitionResolution(CreditResolution.PENDING)
      assertEquals(CreditResolution.PENDING, credit.resolution)
    }

    @Test
    fun `invalid transition from initial to credited is rejected`() {
      val credit = createCredit(resolution = CreditResolution.INITIAL)
      assertThrows<InvalidCreditStateException> {
        credit.transitionResolution(CreditResolution.CREDITED)
      }
    }
  }

  @Nested
  @DisplayName("CRD-007: received_at timestamp")
  inner class ReceivedAtField {

    @Test
    fun `received_at stores datetime when credit received`() {
      val receivedAt = LocalDateTime.of(2024, 3, 15, 10, 30, 0)
      val credit = createCredit(receivedAt = receivedAt)
      assertEquals(receivedAt, credit.receivedAt)
    }

    @Test
    fun `received_at is nullable`() {
      val credit = createCredit(receivedAt = null)
      assertNull(credit.receivedAt)
    }
  }

  @Nested
  @DisplayName("CRD-008: reconciled flag")
  inner class ReconciledFlag {

    @Test
    fun `reconciled defaults to false`() {
      val credit = createCredit()
      assertFalse(credit.reconciled)
    }

    @Test
    fun `reconciled can be set to true`() {
      val credit = createCredit(reconciled = true)
      assertTrue(credit.reconciled)
    }
  }

  @Nested
  @DisplayName("CRD-009: Credit has owner")
  inner class OwnerField {

    @Test
    fun `owner is nullable`() {
      val credit = createCredit(owner = null)
      assertNull(credit.owner)
    }

    @Test
    fun `owner stores username when set`() {
      val credit = createCredit(owner = "clerk1")
      assertEquals("clerk1", credit.owner)
    }
  }

  @Nested
  @DisplayName("CRD-012: blocked flag")
  inner class BlockedFlag {

    @Test
    fun `blocked defaults to false`() {
      val credit = createCredit()
      assertFalse(credit.blocked)
    }

    @Test
    fun `blocked can be set to true`() {
      val credit = createCredit(blocked = true)
      assertTrue(credit.blocked)
    }
  }

  @Nested
  @DisplayName("CRD-013: reviewed flag")
  inner class ReviewedFlag {

    @Test
    fun `reviewed defaults to false`() {
      val credit = createCredit()
      assertFalse(credit.reviewed)
    }

    @Test
    fun `reviewed can be set to true`() {
      val credit = createCredit(reviewed = true)
      assertTrue(credit.reviewed)
    }
  }

  @Nested
  @DisplayName("Timestamps auto-populated")
  inner class Timestamps {

    @Test
    fun `created timestamp defaults to null before persistence`() {
      val credit = createCredit()
      assertNull(credit.created)
    }

    @Test
    fun `modified timestamp defaults to null before persistence`() {
      val credit = createCredit()
      assertNull(credit.modified)
    }

    @Test
    fun `prePersist sets created and modified timestamps`() {
      val credit = createCredit()
      credit.onCreate()
      assertNotNull(credit.created)
      assertNotNull(credit.modified)
    }

    @Test
    fun `preUpdate updates modified timestamp`() {
      val credit = createCredit()
      credit.onCreate()
      val originalModified = credit.modified
      assertNotNull(originalModified)

      credit.onUpdate()
      assertNotNull(credit.modified)
      assertTrue(credit.modified!! >= originalModified)
    }
  }

  @Nested
  @DisplayName("CRD-004: Credit has source type")
  inner class SourceType {

    @Test
    fun `source defaults to UNKNOWN`() {
      val credit = createCredit()
      assertEquals(CreditSource.UNKNOWN, credit.source)
    }
  }

  @Nested
  @DisplayName("String representation")
  inner class StringRepresentation {

    @Test
    fun `toString returns amount and prisoner number`() {
      val credit = createCredit(amount = 1234, prisonerNumber = "A1234BC")
      assertEquals("Credit(A1234BC, £12.34, PENDING)", credit.toString())
    }

    @Test
    fun `toString handles null prisoner number`() {
      val credit = createCredit(amount = 500, prisonerNumber = null)
      assertEquals("Credit(null, £5.00, PENDING)", credit.toString())
    }
  }
}
