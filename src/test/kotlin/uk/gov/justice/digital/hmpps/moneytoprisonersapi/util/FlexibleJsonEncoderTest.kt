package uk.gov.justice.digital.hmpps.moneytoprisonersapi.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@DisplayName("FlexibleJsonEncoder")
class FlexibleJsonEncoderTest {

  @Nested
  @DisplayName("UEL-004: Never raises TypeError; uses pk or str() fallback")
  inner class NeverRaisesError {

    @Test
    fun `encodes null as JSON null`() {
      val result = FlexibleJsonEncoder.encode(null)
      assertEquals("null", result)
    }

    @Test
    fun `encodes String`() {
      val result = FlexibleJsonEncoder.encode("hello")
      assertEquals("\"hello\"", result)
    }

    @Test
    fun `encodes Int`() {
      val result = FlexibleJsonEncoder.encode(42)
      assertEquals("42", result)
    }

    @Test
    fun `encodes Boolean`() {
      assertEquals("true", FlexibleJsonEncoder.encode(true))
      assertEquals("false", FlexibleJsonEncoder.encode(false))
    }

    @Test
    fun `encodes Map as JSON object`() {
      val result = FlexibleJsonEncoder.encode(mapOf("key" to "value"))
      assertEquals("""{"key":"value"}""", result)
    }

    @Test
    fun `encodes List as JSON array`() {
      val result = FlexibleJsonEncoder.encode(listOf(1, 2, 3))
      assertEquals("[1,2,3]", result)
    }

    @Test
    fun `encodes object with id property using its id (pk fallback)`() {
      data class Entity(val id: Long = 99L, val name: String = "test")
      val result = FlexibleJsonEncoder.encode(Entity())
      // Should encode to JSON (Jackson handles data classes natively)
      assertTrue(result.contains("99"))
    }

    @Test
    fun `never throws for any object type`() {
      // An anonymous object with no serializable fields encodes as {} by Jackson —
      // the encoder must not throw regardless of what Jackson produces.
      val result = FlexibleJsonEncoder.encode(object : Any() {
        override fun toString() = "custom-object"
      })
      assertTrue(result.isNotBlank(), "Result should not be blank; got: $result")
    }

    @Test
    fun `encodes object with id property by id when normal serialization fails`() {
      // Simulate a JPA entity-like object with only an id and no serializable state
      val entity = object {
        val id: Long = 42L
        override fun toString() = "Entity(id=42)"
      }
      val result = FlexibleJsonEncoder.encode(entity)
      // The result should reference the id (42) as the primary key fallback
      assertTrue(result.contains("42"))
    }
  }

  @Nested
  @DisplayName("UEL-005: DateTime serialized as ISO 8601 with timezone info")
  inner class DateTimeSerialization {

    @Test
    fun `encodes ZonedDateTime as ISO 8601 with timezone`() {
      val dt = ZonedDateTime.of(2024, 3, 15, 10, 30, 0, 0, ZoneOffset.UTC)
      val result = FlexibleJsonEncoder.encode(dt)
      // Must contain ISO 8601 date/time and timezone
      assertTrue(result.contains("2024-03-15T10:30:00"), "Expected ISO date-time but got: $result")
      // UTC timezone indicated by Z or +00:00
      assertTrue(result.contains("Z") || result.contains("+00:00"), "Expected timezone info but got: $result")
    }

    @Test
    fun `encodes OffsetDateTime as ISO 8601 with timezone`() {
      val dt = OffsetDateTime.of(2024, 6, 1, 14, 0, 0, 0, ZoneOffset.ofHours(1))
      val result = FlexibleJsonEncoder.encode(dt)
      assertTrue(result.contains("2024-06-01T14:00:00"), "Expected ISO date-time but got: $result")
      assertTrue(result.contains("+01:00"), "Expected +01:00 offset but got: $result")
    }

    @Test
    fun `encodes ZonedDateTime not as numeric timestamp`() {
      val dt = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val result = FlexibleJsonEncoder.encode(dt)
      assertFalse(result.all { it.isDigit() || it == '"' }, "Should not be a raw numeric timestamp: $result")
    }

    @Test
    fun `encodes map containing ZonedDateTime values`() {
      val dt = ZonedDateTime.of(2024, 3, 15, 10, 30, 0, 0, ZoneOffset.UTC)
      val result = FlexibleJsonEncoder.encode(mapOf("at" to dt))
      assertTrue(result.contains("2024-03-15T10:30:00"), "Expected ISO date-time in map but got: $result")
    }
  }
}
