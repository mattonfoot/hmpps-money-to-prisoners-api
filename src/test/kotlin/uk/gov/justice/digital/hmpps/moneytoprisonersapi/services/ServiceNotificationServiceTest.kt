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
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.ServiceNotification
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.ServiceNotificationRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("ServiceNotificationService")
class ServiceNotificationServiceTest {

  @Mock
  private lateinit var serviceNotificationRepository: ServiceNotificationRepository

  @InjectMocks
  private lateinit var serviceNotificationService: ServiceNotificationService

  private val now = LocalDateTime.now()

  private fun makeNotification(
    target: String = "cashbook_login",
    level: Int = 20,
    public: Boolean = false,
    headline: String = "Test",
    message: String = "",
    start: LocalDateTime = now.minusHours(1),
    end: LocalDateTime? = now.plusHours(1),
  ) = ServiceNotification(
    target = target,
    level = level,
    public = public,
    headline = headline,
    message = message,
    start = start,
    end = end,
  )

  // -------------------------------------------------------------------------
  // SVC-010: Lists active notifications
  // -------------------------------------------------------------------------

  @Test
  fun `SVC-010 returns all active notifications for authenticated user`() {
    val n1 = makeNotification(public = false)
    val n2 = makeNotification(public = true)
    whenever(serviceNotificationRepository.findActive(any())).thenReturn(listOf(n1, n2))

    val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = null)

    assertThat(result).hasSize(2)
  }

  // -------------------------------------------------------------------------
  // SVC-011: Public notifications visible without auth
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("public visibility (SVC-011)")
  inner class PublicVisibility {

    @Test
    fun `SVC-011 unauthenticated request only returns public notifications`() {
      val publicNotification = makeNotification(public = true)
      val privateNotification = makeNotification(public = false)
      whenever(serviceNotificationRepository.findActive(any())).thenReturn(listOf(publicNotification, privateNotification))

      val result = serviceNotificationService.listNotifications(authenticated = false, targetPrefix = null)

      assertThat(result).hasSize(1)
      assertThat(result[0].public).isTrue()
    }

    @Test
    fun `SVC-011 authenticated request returns both public and private notifications`() {
      val publicNotification = makeNotification(public = true)
      val privateNotification = makeNotification(public = false)
      whenever(serviceNotificationRepository.findActive(any())).thenReturn(listOf(publicNotification, privateNotification))

      val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = null)

      assertThat(result).hasSize(2)
    }
  }

  // -------------------------------------------------------------------------
  // SVC-012: Filter by target prefix
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("target prefix filter (SVC-012)")
  inner class TargetPrefixFilter {

    @Test
    fun `SVC-012 filters by target prefix`() {
      val cashbookLogin = makeNotification(target = "cashbook_login")
      val cashbookDashboard = makeNotification(target = "cashbook_dashboard")
      val nomsOps = makeNotification(target = "noms_ops_login")
      whenever(serviceNotificationRepository.findActive(any()))
        .thenReturn(listOf(cashbookLogin, cashbookDashboard, nomsOps))

      val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = "cashbook")

      assertThat(result).hasSize(2)
      assertThat(result.map { it.target }).containsExactlyInAnyOrder("cashbook_login", "cashbook_dashboard")
    }

    @Test
    fun `SVC-012 returns all when no target prefix`() {
      val n1 = makeNotification(target = "cashbook_login")
      val n2 = makeNotification(target = "noms_ops_login")
      whenever(serviceNotificationRepository.findActive(any())).thenReturn(listOf(n1, n2))

      val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = null)

      assertThat(result).hasSize(2)
    }
  }

  // -------------------------------------------------------------------------
  // SVC-013: Notification level mapped to string label
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("level label mapping (SVC-013)")
  inner class LevelLabelMapping {

    @Test
    fun `SVC-013 level 20 maps to info`() {
      whenever(serviceNotificationRepository.findActive(any())).thenReturn(listOf(makeNotification(level = 20)))

      val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = null)

      assertThat(result[0].level).isEqualTo("info")
    }

    @Test
    fun `SVC-013 level 25 maps to success`() {
      whenever(serviceNotificationRepository.findActive(any())).thenReturn(listOf(makeNotification(level = 25)))

      val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = null)

      assertThat(result[0].level).isEqualTo("success")
    }

    @Test
    fun `SVC-013 level 30 maps to warning`() {
      whenever(serviceNotificationRepository.findActive(any())).thenReturn(listOf(makeNotification(level = 30)))

      val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = null)

      assertThat(result[0].level).isEqualTo("warning")
    }

    @Test
    fun `SVC-013 level 40 maps to error`() {
      whenever(serviceNotificationRepository.findActive(any())).thenReturn(listOf(makeNotification(level = 40)))

      val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = null)

      assertThat(result[0].level).isEqualTo("error")
    }
  }

  // -------------------------------------------------------------------------
  // SVC-014: Active = now between start and end
  // -------------------------------------------------------------------------

  @Test
  fun `SVC-014 returns empty list when repository finds no active notifications`() {
    whenever(serviceNotificationRepository.findActive(any())).thenReturn(emptyList())

    val result = serviceNotificationService.listNotifications(authenticated = true, targetPrefix = null)

    assertThat(result).isEmpty()
  }
}
