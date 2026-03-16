package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.LocalDate

@DisplayName("Balance Model")
class BalanceTest {

  @Nested
  @DisplayName("ACC-001: Balance stores closing amount as integer (pence)")
  inner class ClosingBalanceField {

    @Test
    fun `closing_balance stores value as BigInteger`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(12345),
        date = LocalDate.of(2024, 1, 15),
      )
      assertEquals(BigInteger.valueOf(12345), balance.closingBalance)
    }

    @Test
    fun `closing_balance handles large values`() {
      val largeValue = BigInteger("999999999999999999")
      val balance = Balance(
        closingBalance = largeValue,
        date = LocalDate.of(2024, 1, 15),
      )
      assertEquals(largeValue, balance.closingBalance)
    }

    @Test
    fun `closing_balance handles zero`() {
      val balance = Balance(
        closingBalance = BigInteger.ZERO,
        date = LocalDate.of(2024, 1, 15),
      )
      assertEquals(BigInteger.ZERO, balance.closingBalance)
    }
  }

  @Nested
  @DisplayName("ACC-002: Balance records date")
  inner class DateField {

    @Test
    fun `date field is LocalDate (date-only, no time)`() {
      val date = LocalDate.of(2024, 3, 20)
      val balance = Balance(
        closingBalance = BigInteger.valueOf(1000),
        date = date,
      )
      assertEquals(date, balance.date)
    }
  }

  @Nested
  @DisplayName("ACC-003: Timestamps auto-populated")
  inner class Timestamps {

    @Test
    fun `created timestamp defaults to null before persistence`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(1000),
        date = LocalDate.of(2024, 1, 15),
      )
      // Before persistence, created is null
      assertEquals(null, balance.created)
    }

    @Test
    fun `modified timestamp defaults to null before persistence`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(1000),
        date = LocalDate.of(2024, 1, 15),
      )
      // Before persistence, modified is null
      assertEquals(null, balance.modified)
    }

    @Test
    fun `prePersist sets created and modified timestamps`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(1000),
        date = LocalDate.of(2024, 1, 15),
      )
      balance.onCreate()
      assertNotNull(balance.created)
      assertNotNull(balance.modified)
    }

    @Test
    fun `preUpdate updates modified timestamp`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(1000),
        date = LocalDate.of(2024, 1, 15),
      )
      balance.onCreate()
      val originalModified = balance.modified
      assertNotNull(originalModified)

      // Simulate time passing
      balance.onUpdate()
      assertNotNull(balance.modified)
      assertTrue(balance.modified!! >= originalModified)
    }
  }

  @Nested
  @DisplayName("ACC-005: String representation includes formatted currency")
  inner class StringRepresentation {

    @Test
    fun `toString returns date and formatted currency`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(1234),
        date = LocalDate.of(2024, 1, 15),
      )
      assertEquals("2024-01-15 £12.34", balance.toString())
    }

    @Test
    fun `toString formats zero balance correctly`() {
      val balance = Balance(
        closingBalance = BigInteger.ZERO,
        date = LocalDate.of(2024, 6, 1),
      )
      assertEquals("2024-06-01 £0.00", balance.toString())
    }

    @Test
    fun `toString formats single digit pence correctly`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(5),
        date = LocalDate.of(2024, 12, 25),
      )
      assertEquals("2024-12-25 £0.05", balance.toString())
    }

    @Test
    fun `toString formats large amounts correctly`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(1000000),
        date = LocalDate.of(2024, 7, 4),
      )
      assertEquals("2024-07-04 £10000.00", balance.toString())
    }
  }
}
