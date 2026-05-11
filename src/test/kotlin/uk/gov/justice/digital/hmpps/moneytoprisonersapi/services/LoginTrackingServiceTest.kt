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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FailedLoginAttempt
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUserLogin
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.OAuthApplication
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.FailedLoginAttemptRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserLoginRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.OAuthApplicationRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("LoginTrackingService")
class LoginTrackingServiceTest {

  @Mock
  private lateinit var failedLoginAttemptRepository: FailedLoginAttemptRepository

  @Mock
  private lateinit var mtpUserLoginRepository: MtpUserLoginRepository

  @Mock
  private lateinit var oauthApplicationRepository: OAuthApplicationRepository

  @InjectMocks
  private lateinit var loginTrackingService: LoginTrackingService

  private fun makeUser(username: String = "testuser") = MtpUser(id = 1L, username = username)

  private fun mockApplicationResolved(clientId: String = "cashbook"): OAuthApplication {
    val app = OAuthApplication().apply {
      this.id = 1L
      this.clientId = clientId
    }
    whenever(oauthApplicationRepository.findByClientId(clientId)).thenReturn(app)
    return app
  }

  @Nested
  @DisplayName("recordFailedLogin (AUTH-002)")
  inner class RecordFailedLogin {

    @Test
    fun `AUTH-002 saves a FailedLoginAttempt for the user and application`() {
      val user = makeUser()
      val app = mockApplicationResolved()
      val captor = argumentCaptor<FailedLoginAttempt>()

      loginTrackingService.recordFailedLogin(user, "cashbook")

      verify(failedLoginAttemptRepository).save(captor.capture())
      assertThat(captor.firstValue.user).isEqualTo(user)
      assertThat(captor.firstValue.application).isEqualTo(app)
      assertThat(captor.firstValue.created).isNotNull()
    }

    @Test
    fun `AUTH-006 does not record failed login for service accounts`() {
      val serviceAccount = makeUser(SERVICE_ACCOUNT_USERNAMES.first())

      loginTrackingService.recordFailedLogin(serviceAccount, "cashbook")

      verify(failedLoginAttemptRepository, never()).save(any())
    }
  }

  @Nested
  @DisplayName("isLocked (AUTH-003)")
  inner class IsLocked {

    @Test
    fun `AUTH-003 returns true when failed attempts in period meet or exceed threshold`() {
      val user = makeUser()
      val app = mockApplicationResolved()
      whenever(
        failedLoginAttemptRepository.countByUserAndApplicationAndCreatedAfter(eq(user), eq(app), any()),
      ).thenReturn(LOCKOUT_COUNT.toLong())

      assertThat(loginTrackingService.isLocked(user, "cashbook")).isTrue()
    }

    @Test
    fun `AUTH-003 returns false when failed attempts are below threshold`() {
      val user = makeUser()
      val app = mockApplicationResolved()
      whenever(
        failedLoginAttemptRepository.countByUserAndApplicationAndCreatedAfter(eq(user), eq(app), any()),
      ).thenReturn((LOCKOUT_COUNT - 1).toLong())

      assertThat(loginTrackingService.isLocked(user, "cashbook")).isFalse()
    }

    @Test
    fun `AUTH-003 returns false when the application can't be resolved`() {
      val user = makeUser()
      whenever(oauthApplicationRepository.findByClientId("unknown-app")).thenReturn(null)

      assertThat(loginTrackingService.isLocked(user, "unknown-app")).isFalse()
    }
  }

  @Nested
  @DisplayName("recordLogin (AUTH-005)")
  inner class RecordLogin {

    @Test
    fun `AUTH-005 saves a MtpUserLogin for the user and application`() {
      val user = makeUser()
      val app = mockApplicationResolved()
      val captor = argumentCaptor<MtpUserLogin>()

      loginTrackingService.recordLogin(user, "cashbook")

      verify(mtpUserLoginRepository).save(captor.capture())
      assertThat(captor.firstValue.user).isEqualTo(user)
      assertThat(captor.firstValue.application).isEqualTo(app)
      assertThat(captor.firstValue.created).isNotNull()
    }

    @Test
    fun `AUTH-006 does not record login for service accounts`() {
      val serviceAccount = makeUser(SERVICE_ACCOUNT_USERNAMES.first())

      loginTrackingService.recordLogin(serviceAccount, "cashbook")

      verify(mtpUserLoginRepository, never()).save(any())
    }
  }

  @Nested
  @DisplayName("unlockUser (AUTH-017)")
  inner class UnlockUser {

    @Test
    fun `clears all failed login attempts for the user`() {
      val user = makeUser()

      loginTrackingService.unlockUser(user)

      verify(failedLoginAttemptRepository).deleteByUser(user)
    }
  }

  @Nested
  @DisplayName("clearFailedAttempts (AUTH-042)")
  inner class ClearFailedAttempts {

    @Test
    fun `clears failed login attempts for user and specific application`() {
      val user = makeUser()
      val app = mockApplicationResolved()

      loginTrackingService.clearFailedAttempts(user, "cashbook")

      verify(failedLoginAttemptRepository).deleteByUserAndApplication(user, app)
    }

    @Test
    fun `clears all failed attempts when application is null`() {
      val user = makeUser()

      loginTrackingService.clearFailedAttempts(user, null)

      verify(failedLoginAttemptRepository).deleteByUser(user)
    }
  }
}
