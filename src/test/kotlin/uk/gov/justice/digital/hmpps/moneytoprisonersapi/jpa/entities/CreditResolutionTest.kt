package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CreditResolution Enum")
class CreditResolutionTest {

  @Nested
  @DisplayName("CRD-005: Resolution values")
  inner class ResolutionValues {

    @Test
    fun `has all expected resolution values`() {
      val expected = setOf("initial", "pending", "manual", "credited", "refunded", "failed")
      val actual = CreditResolution.entries.map { it.value }.toSet()
      assertEquals(expected, actual)
    }

    @Test
    fun `has exactly 6 resolution values`() {
      assertEquals(6, CreditResolution.entries.size)
    }
  }

  @Nested
  @DisplayName("CRD-006: Valid transitions")
  inner class ValidTransitions {

    @Test
    fun `initial can transition to pending`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.INITIAL, CreditResolution.PENDING))
    }

    @Test
    fun `initial can transition to failed`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.INITIAL, CreditResolution.FAILED))
    }

    @Test
    fun `initial cannot transition to credited`() {
      assertFalse(CreditResolution.isValidTransition(CreditResolution.INITIAL, CreditResolution.CREDITED))
    }

    @Test
    fun `initial cannot transition to refunded`() {
      assertFalse(CreditResolution.isValidTransition(CreditResolution.INITIAL, CreditResolution.REFUNDED))
    }

    @Test
    fun `pending can transition to manual`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.PENDING, CreditResolution.MANUAL))
    }

    @Test
    fun `pending can transition to credited`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.PENDING, CreditResolution.CREDITED))
    }

    @Test
    fun `pending can transition to refunded`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.PENDING, CreditResolution.REFUNDED))
    }

    @Test
    fun `pending can transition to failed`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.PENDING, CreditResolution.FAILED))
    }

    @Test
    fun `manual can transition to credited`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.MANUAL, CreditResolution.CREDITED))
    }

    @Test
    fun `manual can transition to refunded`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.MANUAL, CreditResolution.REFUNDED))
    }

    @Test
    fun `manual can transition to failed`() {
      assertTrue(CreditResolution.isValidTransition(CreditResolution.MANUAL, CreditResolution.FAILED))
    }
  }

  @Nested
  @DisplayName("Terminal states have no transitions")
  inner class TerminalStates {

    @Test
    fun `credited cannot transition to any state`() {
      CreditResolution.entries.forEach { target ->
        assertFalse(CreditResolution.isValidTransition(CreditResolution.CREDITED, target))
      }
    }

    @Test
    fun `refunded cannot transition to any state`() {
      CreditResolution.entries.forEach { target ->
        assertFalse(CreditResolution.isValidTransition(CreditResolution.REFUNDED, target))
      }
    }

    @Test
    fun `failed cannot transition to any state`() {
      CreditResolution.entries.forEach { target ->
        assertFalse(CreditResolution.isValidTransition(CreditResolution.FAILED, target))
      }
    }

    @Test
    fun `TERMINAL_STATES contains credited, refunded, failed`() {
      assertEquals(
        setOf(CreditResolution.CREDITED, CreditResolution.REFUNDED, CreditResolution.FAILED),
        CreditResolution.TERMINAL_STATES,
      )
    }
  }
}
