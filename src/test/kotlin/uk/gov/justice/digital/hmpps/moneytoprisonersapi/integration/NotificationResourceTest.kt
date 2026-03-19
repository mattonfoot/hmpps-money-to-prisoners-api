package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailFrequency
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailNotificationPreferences
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Event
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.EmailNotificationPreferencesRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.EventRepository
import java.time.LocalDateTime

class NotificationResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var eventRepository: EventRepository

  @Autowired
  private lateinit var emailPreferencesRepository: EmailNotificationPreferencesRepository

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @BeforeEach
  fun setUp() {
    eventRepository.deleteAll()
    emailPreferencesRepository.deleteAll()
  }

  private fun saveEvent(
    rule: String = "MONP",
    username: String? = "AUTH_ADM",
    triggeredAt: LocalDateTime = LocalDateTime.of(2024, 1, 15, 10, 0),
    credit: Credit? = null,
  ): Event = eventRepository.save(
    Event(
      rule = rule,
      description = "Test event",
      triggeredAt = triggeredAt,
      username = username,
      credit = credit,
    ),
  )

  // -------------------------------------------------------------------------
  // NOT-003 to NOT-006: GET /events/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /events/ (NOT-003 to NOT-006)")
  inner class ListEvents {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/events/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("NOT-003 returns user's own events and global events")
    fun `should return own events and global events`() {
      saveEvent(username = "AUTH_ADM")
      saveEvent(username = null) // global
      saveEvent(username = "other_user") // another user's — should not appear

      webTestClient.get()
        .uri("/events/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("NOT-004 filters events by rule code")
    fun `should filter by rule code`() {
      saveEvent(rule = "MONP", username = "AUTH_ADM")
      saveEvent(rule = "MONS", username = "AUTH_ADM")

      webTestClient.get()
        .uri("/events/?rule=MONP")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].rule").isEqualTo("MONP")
    }

    @Test
    @DisplayName("NOT-004 filters by multiple rule codes")
    fun `should filter by multiple rule codes`() {
      saveEvent(rule = "MONP", username = "AUTH_ADM")
      saveEvent(rule = "MONS", username = "AUTH_ADM")
      saveEvent(rule = "NWN", username = "AUTH_ADM")

      webTestClient.get()
        .uri("/events/?rule=MONP&rule=MONS")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("NOT-005 filters events by triggered_at__gte")
    fun `should filter by triggered_at gte`() {
      saveEvent(triggeredAt = LocalDateTime.of(2024, 1, 10, 0, 0))
      saveEvent(triggeredAt = LocalDateTime.of(2024, 1, 20, 0, 0))

      webTestClient.get()
        .uri("/events/?triggered_at__gte=2024-01-15T00:00:00")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("NOT-005 filters events by triggered_at__lt")
    fun `should filter by triggered_at lt`() {
      saveEvent(triggeredAt = LocalDateTime.of(2024, 1, 10, 0, 0))
      saveEvent(triggeredAt = LocalDateTime.of(2024, 1, 20, 0, 0))

      webTestClient.get()
        .uri("/events/?triggered_at__lt=2024-01-15T00:00:00")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("NOT-006 events are ordered by triggered_at desc then id asc")
    fun `should return events ordered by triggered_at desc then id asc`() {
      val t1 = LocalDateTime.of(2024, 1, 15, 10, 0)
      val t2 = LocalDateTime.of(2024, 1, 16, 10, 0)
      saveEvent(triggeredAt = t1)
      saveEvent(triggeredAt = t2)

      val body = webTestClient.get()
        .uri("/events/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody(Map::class.java)
        .returnResult().responseBody!!

      @Suppress("UNCHECKED_CAST")
      val results = body["results"] as List<Map<String, Any>>
      // t2 (newer) should come first
      assertThat(results[0]["triggered_at"].toString()).startsWith("2024-01-16")
      assertThat(results[1]["triggered_at"].toString()).startsWith("2024-01-15")
    }

    @Test
    @DisplayName("Event DTO includes credit_id when event has linked credit")
    fun `should include credit_id in event dto`() {
      val credit = creditRepository.save(Credit(amount = 1000))
      saveEvent(credit = credit)

      webTestClient.get()
        .uri("/events/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].credit_id").isEqualTo(credit.id!!.toInt())
    }

    @Test
    @DisplayName("Event DTO has null credit_id when no credit linked")
    fun `should return null credit_id when no credit`() {
      saveEvent(credit = null)

      webTestClient.get()
        .uri("/events/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].credit_id").doesNotExist()
    }
  }

  // -------------------------------------------------------------------------
  // NOT-007: GET /events/pages/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /events/pages/ (NOT-007)")
  inner class EventPages {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/events/pages/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("NOT-007 returns oldest and newest event dates with count")
    fun `should return oldest and newest dates with count`() {
      saveEvent(triggeredAt = LocalDateTime.of(2024, 1, 10, 0, 0))
      saveEvent(triggeredAt = LocalDateTime.of(2024, 1, 15, 0, 0))
      saveEvent(triggeredAt = LocalDateTime.of(2024, 1, 20, 0, 0))

      webTestClient.get()
        .uri("/events/pages/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.newest").isEqualTo("2024-01-20")
        .jsonPath("$.oldest").isEqualTo("2024-01-10")
        .jsonPath("$.count").isEqualTo(3)
    }

    @Test
    @DisplayName("NOT-007 returns null dates when no events")
    fun `should return null dates when no events`() {
      webTestClient.get()
        .uri("/events/pages/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.newest").doesNotExist()
        .jsonPath("$.oldest").doesNotExist()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("NOT-007 includes only events visible to the user")
    fun `should include only user visible events in pages`() {
      saveEvent(username = "AUTH_ADM", triggeredAt = LocalDateTime.of(2024, 1, 10, 0, 0))
      saveEvent(username = "other_user", triggeredAt = LocalDateTime.of(2024, 1, 20, 0, 0))

      webTestClient.get()
        .uri("/events/pages/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.newest").isEqualTo("2024-01-10")
    }
  }

  // -------------------------------------------------------------------------
  // NOT-008: GET /rules/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /rules/ (NOT-008)")
  inner class ListRules {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/rules/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("NOT-008 returns enabled rules MONP and MONS with descriptions")
    fun `should return enabled rules with descriptions`() {
      webTestClient.get()
        .uri("/rules/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results[?(@.code == 'MONP')].description").isNotEmpty
        .jsonPath("$.results[?(@.code == 'MONS')].description").isNotEmpty
    }
  }

  // -------------------------------------------------------------------------
  // NOT-010 to NOT-012: GET and POST /emailpreferences/
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /emailpreferences/ (NOT-010)")
  inner class GetEmailPreferences {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/emailpreferences/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("NOT-010 returns never when no preference set")
    fun `should return never when no preference set`() {
      webTestClient.get()
        .uri("/emailpreferences/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.frequency").isEqualTo("never")
    }

    @Test
    @DisplayName("NOT-010 returns stored frequency when preference exists")
    fun `should return stored frequency`() {
      emailPreferencesRepository.save(
        EmailNotificationPreferences(username = "AUTH_ADM", frequency = EmailFrequency.DAILY),
      )

      webTestClient.get()
        .uri("/emailpreferences/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.frequency").isEqualTo("daily")
    }
  }

  @Nested
  @DisplayName("POST /emailpreferences/ (NOT-011 to NOT-012)")
  inner class SetEmailPreferences {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.post()
        .uri("/emailpreferences/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"frequency": "daily"}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("NOT-011 sets frequency to daily and returns 204")
    fun `should set frequency to daily`() {
      webTestClient.post()
        .uri("/emailpreferences/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"frequency": "daily"}""")
        .exchange()
        .expectStatus().isNoContent

      val prefs = emailPreferencesRepository.findByUsername("AUTH_ADM")
      assertThat(prefs).isNotNull
      assertThat(prefs!!.frequency).isEqualTo(EmailFrequency.DAILY)
    }

    @Test
    @DisplayName("NOT-012 creates preference when none exists")
    fun `should create preference when none exists`() {
      webTestClient.post()
        .uri("/emailpreferences/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"frequency": "never"}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(emailPreferencesRepository.findByUsername("AUTH_ADM")).isNotNull
    }

    @Test
    @DisplayName("NOT-012 updates existing preference")
    fun `should update existing preference`() {
      emailPreferencesRepository.save(
        EmailNotificationPreferences(username = "AUTH_ADM", frequency = EmailFrequency.NEVER),
      )

      webTestClient.post()
        .uri("/emailpreferences/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"frequency": "daily"}""")
        .exchange()
        .expectStatus().isNoContent

      val prefs = emailPreferencesRepository.findByUsername("AUTH_ADM")
      assertThat(prefs!!.frequency).isEqualTo(EmailFrequency.DAILY)
    }

    @Test
    @DisplayName("Returns 400 for unrecognised frequency value")
    fun `should return 400 for invalid frequency`() {
      webTestClient.post()
        .uri("/emailpreferences/")
        .headers(setAuthorisation(username = "AUTH_ADM"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"frequency": "weekly"}""")
        .exchange()
        .expectStatus().isBadRequest
    }
  }
}
