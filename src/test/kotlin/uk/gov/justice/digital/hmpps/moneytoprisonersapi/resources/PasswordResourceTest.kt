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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ChangePasswordByTokenRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ResetPasswordRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PasswordResetToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordChangeResult
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordResetResult
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordService
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@DisplayName("PasswordResource")
class PasswordResourceTest {

  @Mock
  private lateinit var passwordService: PasswordService

  @InjectMocks
  private lateinit var passwordResource: PasswordResource

  private fun makeUser() = MtpUser(id = 1L, username = "testuser", email = "test@example.com")
  private fun makeToken(user: MtpUser) = PasswordResetToken(id = 1L, user = user, application = "cashbook")

  @Nested
  @DisplayName("POST /reset_password/ (AUTH-043)")
  inner class ResetPassword {

    @Test
    fun `AUTH-043 returns 204 when token created successfully`() {
      val user = makeUser()
      whenever(passwordService.initiatePasswordReset("testuser", null, "cashbook"))
        .thenReturn(PasswordResetResult.TokenCreated(makeToken(user)))

      val request = ResetPasswordRequest(username = "testuser", application = "cashbook")
      val response = passwordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `returns 404 when user not found`() {
      whenever(passwordService.initiatePasswordReset("unknown", null, "cashbook"))
        .thenReturn(PasswordResetResult.UserNotFound)

      val request = ResetPasswordRequest(username = "unknown", application = "cashbook")
      val response = passwordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `AUTH-047 returns 400 when account is locked`() {
      whenever(passwordService.initiatePasswordReset("testuser", null, "cashbook"))
        .thenReturn(PasswordResetResult.AccountLocked)

      val request = ResetPasswordRequest(username = "testuser", application = "cashbook")
      val response = passwordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `AUTH-048 returns 400 when user has no email`() {
      whenever(passwordService.initiatePasswordReset("testuser", null, "cashbook"))
        .thenReturn(PasswordResetResult.NoEmail)

      val request = ResetPasswordRequest(username = "testuser", application = "cashbook")
      val response = passwordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `AUTH-049 returns 400 when multiple users share same email`() {
      whenever(passwordService.initiatePasswordReset(null, "shared@example.com", "cashbook"))
        .thenReturn(PasswordResetResult.MultipleUsers)

      val request = ResetPasswordRequest(email = "shared@example.com", application = "cashbook")
      val response = passwordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when neither username nor email provided`() {
      val request = ResetPasswordRequest(application = "cashbook")
      val response = passwordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }

  @Nested
  @DisplayName("POST /change_password/ via token (AUTH-045)")
  inner class ChangePasswordByToken {

    @Test
    fun `AUTH-045 returns 204 on success`() {
      val user = makeUser()
      val token = UUID.randomUUID()
      whenever(passwordService.changePasswordByToken(token, "newpass123"))
        .thenReturn(PasswordChangeResult.Success(user))

      val request = ChangePasswordByTokenRequest(token = token.toString(), newPassword = "newpass123")
      val response = passwordResource.changePasswordByToken(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `returns 400 for invalid token`() {
      val token = UUID.randomUUID()
      whenever(passwordService.changePasswordByToken(token, "newpass"))
        .thenReturn(PasswordChangeResult.InvalidToken)

      val request = ChangePasswordByTokenRequest(token = token.toString(), newPassword = "newpass")
      val response = passwordResource.changePasswordByToken(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when token is missing`() {
      val request = ChangePasswordByTokenRequest(token = null, newPassword = "newpass")
      val response = passwordResource.changePasswordByToken(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when newPassword is missing`() {
      val request = ChangePasswordByTokenRequest(token = UUID.randomUUID().toString(), newPassword = null)
      val response = passwordResource.changePasswordByToken(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }
}
