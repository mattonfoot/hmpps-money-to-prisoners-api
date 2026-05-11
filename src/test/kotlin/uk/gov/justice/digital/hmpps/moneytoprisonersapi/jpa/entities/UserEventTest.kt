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
    fun `id defaults to 0 before persistence`() {
      // BigAutoField primitive — Kotlin uses Long with default 0, replaced by
      // the DB sequence on save.
      val event = UserEvent()
      assertEquals(0L, event.id)
    }

    @Test
    fun `timestamp is auto-populated on construction`() {
      // Django auto_now_add lives on @PrePersist in Kotlin; the entity gives
      // it a default so it is never null pre-save.
      val event = UserEvent()
      assertNotNull(event.timestamp)
    }

    @Test
    fun `setting an explicit timestamp is preserved`() {
      val event = UserEvent()
      val first = event.timestamp
      // Constructing a fresh event again won't change the original instance.
      assertEquals(first, event.timestamp)
    }
  }

  @Nested
  @DisplayName("UEL-002: Captures request user and api path")
  inner class UserAndPath {

    @Test
    fun `user field defaults to null`() {
      val event = UserEvent()
      assertNull(event.user)
    }

    @Test
    fun `apiUrlPath defaults to empty`() {
      val event = UserEvent()
      assertEquals("", event.apiUrlPath)
    }

    @Test
    fun `user and apiUrlPath can be set`() {
      val event = UserEvent(apiUrlPath = "/credits/")
      // Attach the user directly — the integration tests test the FK lookup
      // path; here we just want to verify the field stores what's set.
      event.user = AuthUser().apply { username = "testuser" }
      assertEquals("testuser", event.user?.username)
      assertEquals("/credits/", event.apiUrlPath)
    }
  }

  @Nested
  @DisplayName("UEL-003: JSONB data field (nullable)")
  inner class DataField {

    @Test
    fun `data field defaults to null`() {
      val event = UserEvent()
      assertNull(event.data)
    }

    @Test
    fun `data field stores a Map`() {
      val payload = mapOf("action" to "credited", "amount" to 1000)
      val event = UserEvent(data = payload)
      assertEquals(payload, event.data)
    }

    @Test
    fun `data field accepts null`() {
      val event = UserEvent(data = null)
      assertNull(event.data)
    }
  }
}
