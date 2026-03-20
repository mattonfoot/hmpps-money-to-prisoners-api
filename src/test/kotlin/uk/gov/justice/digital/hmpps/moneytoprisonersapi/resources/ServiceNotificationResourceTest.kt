package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ServiceNotificationDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.ServiceNotificationService
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("ServiceNotificationResource")
class ServiceNotificationResourceTest {

  @Mock
  private lateinit var serviceNotificationService: ServiceNotificationService

  @InjectMocks
  private lateinit var resource: ServiceNotificationResource

  private val now = LocalDateTime.now()

  private fun makeNotification(
    target: String = "cashbook_login",
    level: String = "info",
    public: Boolean = false,
    headline: String = "Test notification",
  ) = ServiceNotificationDto(
    target = target,
    level = level,
    start = now.minusHours(1),
    end = now.plusHours(1),
    headline = headline,
    message = "",
    public = public,
  )

  private fun authenticatedToken() = UsernamePasswordAuthenticationToken(
    "user",
    null,
    listOf(SimpleGrantedAuthority("ROLE_USER")),
  )

  private fun anonymousToken() = AnonymousAuthenticationToken(
    "key",
    "anonymousUser",
    listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
  )

  // -------------------------------------------------------------------------
  // SVC-010: GET /notifications/ lists active notifications (date-filtered)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /notifications/ (SVC-010)")
  inner class ListNotifications {

    @Test
    fun `SVC-010 returns active notifications as paginated response`() {
      val notification = makeNotification(public = true)
      whenever(serviceNotificationService.listNotifications(true, null)).thenReturn(listOf(notification))

      val result = resource.listNotifications(null, authenticatedToken())

      assertThat(result.count).isEqualTo(1)
      assertThat(result.results).hasSize(1)
    }

    @Test
    fun `SVC-010 returns empty list when no active notifications`() {
      whenever(serviceNotificationService.listNotifications(true, null)).thenReturn(emptyList())

      val result = resource.listNotifications(null, authenticatedToken())

      assertThat(result.count).isEqualTo(0)
      assertThat(result.results).isEmpty()
    }
  }

  // -------------------------------------------------------------------------
  // SVC-011: Public notifications visible without auth
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("public visibility (SVC-011)")
  inner class PublicVisibility {

    @Test
    fun `SVC-011 unauthenticated request passes authenticated=false to service`() {
      whenever(serviceNotificationService.listNotifications(false, null)).thenReturn(emptyList())

      resource.listNotifications(null, null)

      verify(serviceNotificationService).listNotifications(false, null)
    }

    @Test
    fun `SVC-011 anonymous token passes authenticated=false to service`() {
      whenever(serviceNotificationService.listNotifications(false, null)).thenReturn(emptyList())

      resource.listNotifications(null, anonymousToken())

      verify(serviceNotificationService).listNotifications(false, null)
    }

    @Test
    fun `SVC-011 authenticated request passes authenticated=true to service`() {
      whenever(serviceNotificationService.listNotifications(true, null)).thenReturn(emptyList())

      resource.listNotifications(null, authenticatedToken())

      verify(serviceNotificationService).listNotifications(true, null)
    }
  }

  // -------------------------------------------------------------------------
  // SVC-012: Filter by target prefix
  // -------------------------------------------------------------------------

  @Test
  fun `SVC-012 passes target prefix to service`() {
    whenever(serviceNotificationService.listNotifications(true, "cashbook")).thenReturn(emptyList())

    resource.listNotifications("cashbook", authenticatedToken())

    verify(serviceNotificationService).listNotifications(true, "cashbook")
  }

  // -------------------------------------------------------------------------
  // SVC-013: Notification has level (info, warning, error, success)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("notification level (SVC-013)")
  inner class NotificationLevel {

    @Test
    fun `SVC-013 level is returned as string label`() {
      val notification = makeNotification(level = "warning")
      whenever(serviceNotificationService.listNotifications(true, null)).thenReturn(listOf(notification))

      val result = resource.listNotifications(null, authenticatedToken())

      assertThat(result.results[0].level).isEqualTo("warning")
    }

    @Test
    fun `SVC-013 supports info level`() {
      val notification = makeNotification(level = "info")
      whenever(serviceNotificationService.listNotifications(true, null)).thenReturn(listOf(notification))

      val result = resource.listNotifications(null, authenticatedToken())

      assertThat(result.results[0].level).isEqualTo("info")
    }

    @Test
    fun `SVC-013 supports error level`() {
      val notification = makeNotification(level = "error")
      whenever(serviceNotificationService.listNotifications(true, null)).thenReturn(listOf(notification))

      val result = resource.listNotifications(null, authenticatedToken())

      assertThat(result.results[0].level).isEqualTo("error")
    }

    @Test
    fun `SVC-013 supports success level`() {
      val notification = makeNotification(level = "success")
      whenever(serviceNotificationService.listNotifications(true, null)).thenReturn(listOf(notification))

      val result = resource.listNotifications(null, authenticatedToken())

      assertThat(result.results[0].level).isEqualTo("success")
    }
  }

  // -------------------------------------------------------------------------
  // SVC-014: Active = now between start and end
  // -------------------------------------------------------------------------

  @Test
  fun `SVC-014 response contains target and headline from notification`() {
    val notification = makeNotification(target = "noms_ops_login", headline = "Planned maintenance")
    whenever(serviceNotificationService.listNotifications(true, null)).thenReturn(listOf(notification))

    val result = resource.listNotifications(null, authenticatedToken())

    assertThat(result.results[0].target).isEqualTo("noms_ops_login")
    assertThat(result.results[0].headline).isEqualTo("Planned maintenance")
  }
}
