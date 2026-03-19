package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailFrequency
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailNotificationPreferences
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Event
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.EmailNotificationPreferencesRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.EventRepository
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("NotificationService")
class NotificationServiceTest {

  @Mock
  private lateinit var eventRepository: EventRepository

  @Mock
  private lateinit var emailPreferencesRepository: EmailNotificationPreferencesRepository

  @InjectMocks
  private lateinit var notificationService: NotificationService

  @Nested
  @DisplayName("listEvents (NOT-003 to NOT-006)")
  inner class ListEvents {

    @Test
    fun `NOT-003 delegates to repository with specifications`() {
      whenever(eventRepository.findAll(any(), any<org.springframework.data.domain.Sort>()))
        .thenReturn(emptyList())

      notificationService.listEvents("user1", null, null, null)

      verify(eventRepository).findAll(any(), any<org.springframework.data.domain.Sort>())
    }

    @Test
    fun `NOT-004 passes rule filter via specifications`() {
      val rules = listOf("MONP", "MONS")
      whenever(eventRepository.findAll(any(), any<org.springframework.data.domain.Sort>()))
        .thenReturn(emptyList())

      notificationService.listEvents("user1", rules, null, null)

      verify(eventRepository).findAll(any(), any<org.springframework.data.domain.Sort>())
    }

    @Test
    fun `NOT-004 empty rule list treated same as null`() {
      whenever(eventRepository.findAll(any(), any<org.springframework.data.domain.Sort>()))
        .thenReturn(emptyList())

      notificationService.listEvents("user1", emptyList(), null, null)

      verify(eventRepository).findAll(any(), any<org.springframework.data.domain.Sort>())
    }

    @Test
    fun `NOT-005 passes datetime range via specifications`() {
      val gte = LocalDateTime.of(2024, 1, 1, 0, 0)
      val lt = LocalDateTime.of(2024, 2, 1, 0, 0)
      whenever(eventRepository.findAll(any(), any<org.springframework.data.domain.Sort>()))
        .thenReturn(emptyList())

      notificationService.listEvents("user1", null, gte, lt)

      verify(eventRepository).findAll(any(), any<org.springframework.data.domain.Sort>())
    }

    @Test
    fun `returns events from repository`() {
      val event = Event(id = 1L, rule = "MONP", username = "user1")
      whenever(eventRepository.findAll(any(), any<org.springframework.data.domain.Sort>()))
        .thenReturn(listOf(event))

      val result = notificationService.listEvents("user1", null, null, null)

      assertThat(result).hasSize(1)
      assertThat(result[0].rule).isEqualTo("MONP")
    }
  }

  @Nested
  @DisplayName("getEventPages (NOT-007)")
  inner class GetEventPages {

    @Test
    fun `NOT-007 returns newest and oldest dates with count`() {
      val dates = listOf(
        LocalDate.of(2024, 1, 15),
        LocalDate.of(2024, 1, 14),
        LocalDate.of(2024, 1, 13),
      )
      whenever(eventRepository.findDistinctDatesPaged("user1", null))
        .thenReturn(dates)

      val (newest, oldest, count) = notificationService.getEventPages("user1", null, 0, 25)

      assertThat(newest).isEqualTo("2024-01-15")
      assertThat(oldest).isEqualTo("2024-01-13")
      assertThat(count).isEqualTo(3)
    }

    @Test
    fun `NOT-007 returns null newest and oldest when no events`() {
      whenever(eventRepository.findDistinctDatesPaged("user1", null))
        .thenReturn(emptyList())

      val (newest, oldest, count) = notificationService.getEventPages("user1", null, 0, 25)

      assertThat(newest).isNull()
      assertThat(oldest).isNull()
      assertThat(count).isEqualTo(0)
    }

    @Test
    fun `NOT-007 applies offset and limit to returned dates`() {
      val dates = (1..10).map { LocalDate.of(2024, 1, it) }.reversed()
      whenever(eventRepository.findDistinctDatesPaged("user1", null))
        .thenReturn(dates)

      val (newest, oldest, count) = notificationService.getEventPages("user1", null, 2, 3)

      assertThat(count).isEqualTo(10)
      assertThat(newest).isEqualTo("2024-01-08")
      assertThat(oldest).isEqualTo("2024-01-06")
    }
  }

  @Nested
  @DisplayName("getEmailFrequency (NOT-010)")
  inner class GetEmailFrequency {

    @Test
    fun `NOT-010 returns frequency when preference exists`() {
      val prefs = EmailNotificationPreferences(username = "user1", frequency = EmailFrequency.DAILY)
      whenever(emailPreferencesRepository.findByUsername("user1")).thenReturn(prefs)

      val result = notificationService.getEmailFrequency("user1")

      assertThat(result).isEqualTo("daily")
    }

    @Test
    fun `NOT-010 returns never when no preference set`() {
      whenever(emailPreferencesRepository.findByUsername("user1")).thenReturn(null)

      val result = notificationService.getEmailFrequency("user1")

      assertThat(result).isEqualTo("never")
    }
  }

  @Nested
  @DisplayName("setEmailFrequency (NOT-011 to NOT-012)")
  inner class SetEmailFrequency {

    @Test
    fun `NOT-012 creates new preference when none exists`() {
      whenever(emailPreferencesRepository.findByUsername("user1")).thenReturn(null)

      notificationService.setEmailFrequency("user1", EmailFrequency.DAILY)

      verify(emailPreferencesRepository).save(
        org.mockito.kotlin.argThat<EmailNotificationPreferences> { prefs ->
          prefs.username == "user1" && prefs.frequency == EmailFrequency.DAILY
        },
      )
    }

    @Test
    fun `NOT-012 updates existing preference`() {
      val existing = EmailNotificationPreferences(id = 1L, username = "user1", frequency = EmailFrequency.NEVER)
      whenever(emailPreferencesRepository.findByUsername("user1")).thenReturn(existing)

      notificationService.setEmailFrequency("user1", EmailFrequency.DAILY)

      assertThat(existing.frequency).isEqualTo(EmailFrequency.DAILY)
      verify(emailPreferencesRepository).save(existing)
    }
  }
}
