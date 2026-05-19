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
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PasswordResetToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PasswordResetTokenRepository
import java.security.MessageDigest
import java.util.Base64
import java.util.Optional
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
  ) = MtpUser().apply {
    this.id = 1L
    this.username = username
    this.email = email
    this.isActive = isActive
  }

  private fun djangoPassword(password: String, salt: String = "testsalt", iterations: Int = 600000): String {
    val hash = pbkdf2(password, salt, iterations)
    return listOf("pbkdf2_sha256", iterations.toString(), salt, hash).joinToString("$")
  }

  private fun matchesDjangoPassword(raw: String, encoded: String): Boolean {
    val parts = encoded.split('$')
    if (parts.size != 4 || parts[0] != "pbkdf2_sha256") return false
    val iterations = parts[1].toInt()
    val salt = parts[2]
    val expected = Base64.getDecoder().decode(parts[3])
    val actual = Base64.getDecoder().decode(pbkdf2(raw, salt, iterations))
    return MessageDigest.isEqual(actual, expected)
  }

  private fun pbkdf2(password: String, salt: String, iterations: Int): String {
    val keySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), iterations, 256)
    val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec)
    return Base64.getEncoder().encodeToString(secret.encoded)
  }

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
  @DisplayName("changePassword (AUTH-045)")
  inner class ChangePassword {

    @Test
    fun `AUTH-045 updates stored password and clears failed attempts`() {
      val user = makeUser().apply {
        password = djangoPassword("current-pass")
      }
      whenever(mtpUserRepository.findByUsernameIgnoreCase("testuser")).thenReturn(user)
      whenever(loginTrackingService.isLocked(user, "cashbook")).thenReturn(false)
      whenever(mtpUserRepository.save(user)).thenReturn(user)

      val result = passwordService.changePassword("testuser", "current-pass", "freshpass", "cashbook")

      assertThat(result).isEqualTo(AuthenticatedPasswordChangeResult.Success(user))
      assertThat(matchesDjangoPassword("freshpass", user.password)).isTrue()
      verify(loginTrackingService).clearFailedAttempts(user, "cashbook")
    }

    @Test
    fun `returns IncorrectPassword when old password does not match and records failed attempt`() {
      val user = makeUser().apply {
        password = djangoPassword("current-pass")
      }
      whenever(mtpUserRepository.findByUsernameIgnoreCase("testuser")).thenReturn(user)
      whenever(loginTrackingService.isLocked(user, "cashbook")).thenReturn(false)

      val result = passwordService.changePassword("testuser", "wrong-pass", "freshpass", "cashbook")

      assertThat(result).isEqualTo(AuthenticatedPasswordChangeResult.IncorrectPassword)
      assertThat(matchesDjangoPassword("current-pass", user.password)).isTrue()
      verify(loginTrackingService).recordFailedLogin(user, "cashbook")
      verify(loginTrackingService, never()).clearFailedAttempts(any(), any())
    }

    @Test
    fun `returns IncorrectPassword when account is locked`() {
      val user = makeUser().apply {
        password = djangoPassword("current-pass")
      }
      whenever(mtpUserRepository.findByUsernameIgnoreCase("testuser")).thenReturn(user)
      whenever(loginTrackingService.isLocked(user, "cashbook")).thenReturn(true)

      val result = passwordService.changePassword("testuser", "current-pass", "freshpass", "cashbook")

      assertThat(result).isEqualTo(AuthenticatedPasswordChangeResult.IncorrectPassword)
      assertThat(matchesDjangoPassword("current-pass", user.password)).isTrue()
      verify(loginTrackingService, never()).recordFailedLogin(any(), any())
      verify(loginTrackingService, never()).clearFailedAttempts(any(), any())
    }
  }

  @Nested
  @DisplayName("changePasswordByToken (AUTH-045)")
  inner class ChangePasswordByToken {

    @Test
    fun `AUTH-045 updates stored password and clears failed attempts`() {
      val user = makeUser().apply {
        password = djangoPassword("current-pass")
      }
      val tokenId = UUID.randomUUID()
      val token = PasswordResetToken().apply {
        id = tokenId
        this.user = user
      }
      whenever(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token))
      whenever(mtpUserRepository.save(user)).thenReturn(user)

      val result = passwordService.changePasswordByToken(tokenId, "newpass123")

      assertThat(result).isInstanceOf(PasswordChangeResult.Success::class.java)
      assertThat(matchesDjangoPassword("newpass123", user.password)).isTrue()
      verify(passwordResetTokenRepository, never()).delete(any())
      verify(loginTrackingService).clearFailedAttempts(eq(user), isNull())
    }

    @Test
    fun `returns InvalidToken when token not found`() {
      val unknownToken = UUID.randomUUID()
      whenever(passwordResetTokenRepository.findById(unknownToken)).thenReturn(Optional.empty())

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
