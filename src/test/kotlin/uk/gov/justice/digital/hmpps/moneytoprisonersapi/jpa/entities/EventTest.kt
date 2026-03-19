package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("Event entity")
class EventTest {

  @Test
  fun `NOT-001 event has rule code`() {
    val event = Event(rule = "MONP", description = "Monitored prisoner")
    assertThat(event.rule).isEqualTo("MONP")
  }

  @Test
  fun `NOT-002 user-specific event has non-null username`() {
    val event = Event(rule = "MONP", username = "user1", triggeredAt = LocalDateTime.now())
    assertThat(event.username).isEqualTo("user1")
  }

  @Test
  fun `NOT-002 global event has null username`() {
    val event = Event(rule = "MONS", username = null)
    assertThat(event.username).isNull()
  }

  @Test
  fun `event description defaults to empty string`() {
    val event = Event(rule = "NWN")
    assertThat(event.description).isEmpty()
  }

  @Test
  fun `event can have a credit link`() {
    val credit = Credit(amount = 1000)
    val event = Event(rule = "MONP", credit = credit)
    assertThat(event.credit).isNotNull
    assertThat(event.credit).isEqualTo(credit)
  }

  @Test
  fun `event can have a prisoner profile link`() {
    val profile = PrisonerProfile(prisonerNumber = "A1234BC")
    val event = Event(rule = "MONP", prisonerProfile = profile)
    assertThat(event.prisonerProfile).isNotNull
    assertThat(event.prisonerProfile?.prisonerNumber).isEqualTo("A1234BC")
  }
}
