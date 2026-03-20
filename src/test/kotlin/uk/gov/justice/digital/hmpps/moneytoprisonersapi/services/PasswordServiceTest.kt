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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PasswordResetToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PasswordResetTokenRepository
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@DisplayName("PasswordService")
class PasswordServiceTest {

  @Mock
  private lateinit var mtpUserRepository: MtpUserRepository

  @Mock
  private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

  @Mock
  private lateinit var loginTrackingService: LoginTrackingService

  @InjectMocks
  private lateinit var passwordService: PasswordService

  private fun makeUser(
    username: String = "testuser",
    email: String = "test@example.com",
    isActive: Boolean = true,
  ) = MtpUser(id = 1L, username = username, email = email, isActive = isActive)

  @Nested
  @DisplayName("initiatePasswordReset (AUTH-043)")
  inner class InitiatePasswordReset {

    @Test
    fun `AUTH-043 creates reset token for user found by username`() {
      val user = makeUser()
      whenever(mtpUserRepository.findByUsernameIgnoreCase("testuser")).thenReturn(user)
      whenever(loginTrackingService.isLocked(user, "cashbook")).thenReturn(false)
      whenever(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] }

      val result = passwordService.initiatePasswordReset(username = "testuser", email = null, application = "cashbook")

      assertThat(result).isInstanceOf(PasswordResetResult.TokenCreated::class.java)
      val captor = argumentCaptor<PasswordResetToken>()
      verify(passwordResetTokenRepository).save(captor.capture())
      assertThat(captor.firstValue.user).isEqualTo(user)
      assertThat(captor.firstValue.application).isEqualTo("cashbook")
    }

    @Test
    fun `AUTH-043 creates reset token for user found by email`() {
      val user = makeUser()
      whenever(mtpUserRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(listOf(user))
      whenever(loginTrackingService.isLocked(user, "cashbook")).thenReturn(false)
      whenever(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] }

      val result = passwordService.initiatePasswordReset(username = null, email = "test@example.com", application = "cashbook")

      assertThat(result).isInstanceOf(PasswordResetResult.TokenCreated::class.java)
    }

    @Test
    fun `AUTH-048 returns NoEmail when user has no email`() {
      val user = makeUser(email = "")
      whenever(mtpUserRepository.findByUsernameIgnoreCase("testuser")).thenReturn(user)
      whenever(loginTrackingService.isLocked(user, "cashbook")).thenReturn(false)

      val result = passwordService.initiatePasswordReset("testuser", null, "cashbook")

      assertThat(result).isEqualTo(PasswordResetResult.NoEmail)
    }

    @Test
    fun `AUTH-047 returns AccountLocked when user is locked`() {
      val user = makeUser()
      whenever(mtpUserRepository.findByUsernameIgnoreCase("testuser")).thenReturn(user)
      whenever(loginTrackingService.isLocked(user, "cashbook")).thenReturn(true)

      val result = passwordService.initiatePasswordReset("testuser", null, "cashbook")

      assertThat(result).isEqualTo(PasswordResetResult.AccountLocked)
    }

    @Test
    fun `returns UserNotFound when no user matches`() {
      whenever(mtpUserRepository.findByUsernameIgnoreCase("unknown")).thenReturn(null)

      val result = passwordService.initiatePasswordReset("unknown", null, "cashbook")

      assertThat(result).isEqualTo(PasswordResetResult.UserNotFound)
    }

    @Test
    fun `AUTH-049 returns MultipleUsers when multiple users share same email`() {
      val user1 = makeUser("user1")
      val user2 = makeUser("user2")
      whenever(mtpUserRepository.findByEmailIgnoreCase("shared@example.com")).thenReturn(listOf(user1, user2))

      val result = passwordService.initiatePasswordReset(null, "shared@example.com", "cashbook")

      assertThat(result).isEqualTo(PasswordResetResult.MultipleUsers)
    }
  }

  @Nested
  @DisplayName("changePasswordByToken (AUTH-045)")
  inner class ChangePasswordByToken {

    @Test
    fun `AUTH-045 marks token as used and clears failed attempts`() {
      val user = makeUser()
      val token = PasswordResetToken(id = 1L, user = user, token = UUID.randomUUID(), application = "cashbook")
      whenever(passwordResetTokenRepository.findByTokenAndUsedFalse(token.token)).thenReturn(token)

      val result = passwordService.changePasswordByToken(token.token, "newpass123")

      assertThat(result).isInstanceOf(PasswordChangeResult.Success::class.java)
      assertThat(token.used).isTrue()
      verify(loginTrackingService).clearFailedAttempts(user, "cashbook")
    }

    @Test
    fun `returns InvalidToken when token not found or already used`() {
      val unknownToken = UUID.randomUUID()
      whenever(passwordResetTokenRepository.findByTokenAndUsedFalse(unknownToken)).thenReturn(null)

      val result = passwordService.changePasswordByToken(unknownToken, "newpass")

      assertThat(result).isEqualTo(PasswordChangeResult.InvalidToken)
    }
  }

  @Nested
  @DisplayName("recordFailedPasswordChange (AUTH-041)")
  inner class RecordFailedPasswordChange {

    @Test
    fun `AUTH-041 delegates to loginTrackingService`() {
      val user = makeUser()

      passwordService.recordFailedPasswordChange(user, "cashbook")

      verify(loginTrackingService).recordFailedLogin(user, "cashbook")
    }
  }
}
