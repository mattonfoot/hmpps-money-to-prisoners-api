package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SetEmailPreferencesRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Event
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.NotificationService
import java.security.Principal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("NotificationResource")
class NotificationResourceTest {

  @Mock
  private lateinit var notificationService: NotificationService

  @InjectMocks
  private lateinit var notificationResource: NotificationResource

  private val principal = Principal { "testuser" }

  private fun makeEvent(
    id: Long = 1L,
    rule: String = "MONP",
    username: String? = "testuser",
    triggeredAt: LocalDateTime = LocalDateTime.of(2024, 1, 15, 10, 0),
    credit: Credit? = null,
    prisonerProfile: PrisonerProfile? = null,
  ) = Event(
    id = id,
    rule = rule,
    description = "Test event",
    triggeredAt = triggeredAt,
    username = username,
    credit = credit,
    prisonerProfile = prisonerProfile,
  )

  @Nested
  @DisplayName("GET /events/ (NOT-003 to NOT-006)")
  inner class ListEvents {

    @Test
    fun `NOT-003 returns events for current user including global events`() {
      val userEvent = makeEvent(id = 1, username = "testuser")
      val globalEvent = makeEvent(id = 2, username = null)
      whenever(notificationService.listEvents("testuser", null, null, null))
        .thenReturn(listOf(userEvent, globalEvent))

      val response = notificationResource.listEvents(null, null, null, principal)

      assertThat(response.count).isEqualTo(2)
      assertThat(response.results).hasSize(2)
    }

    @Test
    fun `NOT-004 passes rule filter to service`() {
      val event = makeEvent(rule = "MONP")
      whenever(notificationService.listEvents("testuser", listOf("MONP"), null, null))
        .thenReturn(listOf(event))

      val response = notificationResource.listEvents(listOf("MONP"), null, null, principal)

      assertThat(response.count).isEqualTo(1)
      assertThat(response.results[0].rule).isEqualTo("MONP")
    }

    @Test
    fun `NOT-005 passes triggered_at range filters to service`() {
      val gte = LocalDateTime.of(2024, 1, 1, 0, 0)
      val lt = LocalDateTime.of(2024, 2, 1, 0, 0)
      whenever(notificationService.listEvents("testuser", null, gte, lt))
        .thenReturn(emptyList())

      val response = notificationResource.listEvents(null, gte, lt, principal)

      assertThat(response.count).isEqualTo(0)
    }

    @Test
    fun `event DTO maps credit_id and prisoner_profile from linked entities`() {
      val credit = Credit(id = 42L, amount = 1000)
      val profile = PrisonerProfile(id = 7L, prisonerNumber = "A1234BC")
      val event = makeEvent(id = 1, credit = credit, prisonerProfile = profile)
      whenever(notificationService.listEvents("testuser", null, null, null))
        .thenReturn(listOf(event))

      val response = notificationResource.listEvents(null, null, null, principal)

      assertThat(response.results[0].creditId).isEqualTo(42L)
      assertThat(response.results[0].prisonerProfile).isEqualTo(7L)
    }

    @Test
    fun `event DTO has null credit_id and profile when no links`() {
      val event = makeEvent(id = 1, credit = null, prisonerProfile = null)
      whenever(notificationService.listEvents("testuser", null, null, null))
        .thenReturn(listOf(event))

      val response = notificationResource.listEvents(null, null, null, principal)

      assertThat(response.results[0].creditId).isNull()
      assertThat(response.results[0].prisonerProfile).isNull()
    }

    @Test
    fun `returns empty paginated response when no events`() {
      whenever(notificationService.listEvents("testuser", null, null, null))
        .thenReturn(emptyList())

      val response = notificationResource.listEvents(null, null, null, principal)

      assertThat(response.count).isEqualTo(0)
      assertThat(response.results).isEmpty()
    }
  }

  @Nested
  @DisplayName("GET /rules/ (NOT-008)")
  inner class ListRules {

    @Test
    fun `NOT-008 returns only enabled rules MONP and MONS`() {
      val response = notificationResource.listRules()

      assertThat(response.results).hasSize(2)
      val codes = response.results.map { it.code }
      assertThat(codes).containsExactlyInAnyOrder("MONP", "MONS")
    }

    @Test
    fun `NOT-008 each rule has a description`() {
      val response = notificationResource.listRules()

      response.results.forEach { rule ->
        assertThat(rule.description).isNotBlank()
      }
    }

    @Test
    fun `count matches number of results`() {
      val response = notificationResource.listRules()
      assertThat(response.count).isEqualTo(response.results.size)
    }
  }

  @Nested
  @DisplayName("GET /emailpreferences/ (NOT-010)")
  inner class GetEmailPreferences {

    @Test
    fun `NOT-010 returns frequency from service`() {
      whenever(notificationService.getEmailFrequency("testuser")).thenReturn("daily")

      val response = notificationResource.getEmailPreferences(principal)

      assertThat(response.frequency).isEqualTo("daily")
    }

    @Test
    fun `NOT-010 returns never when no preference set`() {
      whenever(notificationService.getEmailFrequency("testuser")).thenReturn("never")

      val response = notificationResource.getEmailPreferences(principal)

      assertThat(response.frequency).isEqualTo("never")
    }
  }

  @Nested
  @DisplayName("POST /emailpreferences/ (NOT-011 to NOT-012)")
  inner class SetEmailPreferences {

    @Test
    fun `NOT-011 returns 204 for valid frequency daily`() {
      val request = SetEmailPreferencesRequest(frequency = "daily")

      val response = notificationResource.setEmailPreferences(request, principal)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `NOT-011 returns 204 for valid frequency never`() {
      val request = SetEmailPreferencesRequest(frequency = "never")

      val response = notificationResource.setEmailPreferences(request, principal)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `returns 400 for unrecognised frequency value`() {
      val request = SetEmailPreferencesRequest(frequency = "weekly")

      val response = notificationResource.setEmailPreferences(request, principal)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when frequency is null`() {
      val request = SetEmailPreferencesRequest(frequency = null)

      val response = notificationResource.setEmailPreferences(request, principal)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }
}
