package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserEvent Model")
class UserEventTest {

  @Nested
  @DisplayName("UEL-001: UserEvent records user actions (BigAutoField ID, timestamped)")
  inner class BigAutoFieldAndTimestamp {

    @Test
    fun `id is null before persistence`() {
      val event = UserEvent()
      assertNull(event.id)
    }

    @Test
    fun `timestamp is null before persistence`() {
      val event = UserEvent()
      assertNull(event.timestamp)
    }

    @Test
    fun `prePersist sets timestamp automatically`() {
      val event = UserEvent()
      event.onCreate()
      assertNotNull(event.timestamp)
    }

    @Test
    fun `prePersist does not overwrite an already-set timestamp`() {
      val event = UserEvent()
      event.onCreate()
      val first = event.timestamp
      event.onCreate()
      assertEquals(first, event.timestamp)
    }
  }

  @Nested
  @DisplayName("UEL-002: Captures request user and path (auto-populated)")
  inner class UserAndPath {

    @Test
    fun `user field defaults to null`() {
      val event = UserEvent()
      assertNull(event.user)
    }

    @Test
    fun `path field defaults to null`() {
      val event = UserEvent()
      assertNull(event.path)
    }

    @Test
    fun `user and path can be set`() {
      val user = MtpUser(id = 1L, username = "testuser")
      val event = UserEvent(user = user, path = "/credits/")
      assertEquals(user, event.user)
      assertEquals("/credits/", event.path)
    }
  }

  @Nested
  @DisplayName("UEL-003: JSON data field (nullable, stores structured data)")
  inner class DataField {

    @Test
    fun `data field defaults to null`() {
      val event = UserEvent()
      assertNull(event.data)
    }

    @Test
    fun `data field accepts JSON string`() {
      val json = """{"action":"credited","amount":1000}"""
      val event = UserEvent(data = json)
      assertEquals(json, event.data)
    }

    @Test
    fun `data field accepts null`() {
      val event = UserEvent(data = null)
      assertNull(event.data)
    }
  }
}
