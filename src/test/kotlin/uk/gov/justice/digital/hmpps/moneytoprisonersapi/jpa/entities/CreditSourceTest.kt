package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CreditSource Enum")
class CreditSourceTest {

  @Nested
  @DisplayName("CRD-004: Source type values")
  inner class SourceValues {

    @Test
    fun `has bank_transfer value`() {
      assertEquals("bank_transfer", CreditSource.BANK_TRANSFER.value)
    }

    @Test
    fun `has online value`() {
      assertEquals("online", CreditSource.ONLINE.value)
    }

    @Test
    fun `has unknown value`() {
      assertEquals("unknown", CreditSource.UNKNOWN.value)
    }

    @Test
    fun `has exactly 3 source values`() {
      assertEquals(3, CreditSource.entries.size)
    }
  }
}
