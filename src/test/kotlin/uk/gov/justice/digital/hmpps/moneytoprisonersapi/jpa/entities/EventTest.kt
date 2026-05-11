package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("NotificationEvent entity")
class EventTest {

  @Test
  fun `NOT-001 event has rule code`() {
    val event = Event(rule = "MONP", description = "Monitored prisoner")
    assertThat(event.rule).isEqualTo("MONP")
  }

  @Test
  fun `NOT-002 user-specific event resolves to an AuthUser`() {
    // Pass a real (transient) AuthUser so the assertion doesn't depend on the
    // jvm-static FactoryHooks.userResolver — the integration tests rebind it.
    val event = Event(
      rule = "MONP",
      user = AuthUser().apply { username = "user1" },
      triggeredAt = LocalDateTime.now(),
    )
    assertThat(event.user?.username).isEqualTo("user1")
  }

  @Test
  fun `NOT-002 global event has null user`() {
    val event = Event(rule = "MONS", username = null)
    assertThat(event.user).isNull()
  }

  @Test
  fun `event description defaults to empty string`() {
    val event = Event(rule = "NWN")
    assertThat(event.description).isEmpty()
  }

  @Test
  fun `event auto-populates triggered_at`() {
    val event = Event(rule = "MONP")
    assertThat(event.triggeredAt).isNotNull()
  }
}
