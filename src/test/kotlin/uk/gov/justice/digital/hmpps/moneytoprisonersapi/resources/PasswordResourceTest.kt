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
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.DjangoOAuth2Authentication
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ChangePassword
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ChangePasswordByTokenRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ResetPasswordRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PasswordResetToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.AuthenticatedPasswordChangeResult
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
  private lateinit var resetPasswordResource: ResetPasswordResource

  @InjectMocks
  private lateinit var changePasswordResource: ChangePasswordResource

  private fun makeUser() = MtpUser().apply {
    id = 1L
    username = "testuser"
    email = "test@example.com"
  }

  private fun makeToken(user: MtpUser) = PasswordResetToken().apply {
    id = UUID.randomUUID()
    this.user = user
  }

  private fun authenticatedCashbookToken(username: String = "testuser") = DjangoOAuth2Authentication(
    username = username,
    clientId = "cashbook",
    authorities = listOf(SimpleGrantedAuthority("ROLE_PRISON_CLERK")),
  )

  private fun authenticatedSendMoneyToken(username: String = "testuser") = DjangoOAuth2Authentication(
    username = username,
    clientId = "send-money",
    authorities = listOf(SimpleGrantedAuthority("ROLE_SEND_MONEY")),
  )

  @Nested
  @DisplayName("POST /reset_password/ (AUTH-043)")
  inner class ResetPassword {

    @Test
    fun `AUTH-043 returns 204 when token created successfully`() {
      val user = makeUser()
      whenever(passwordService.initiatePasswordReset("testuser", null, "cashbook"))
        .thenReturn(PasswordResetResult.TokenCreated(makeToken(user)))

      val request = ResetPasswordRequest(username = "testuser", application = "cashbook")
      val response = resetPasswordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `returns 400 when user not found (matches Python)`() {
      whenever(passwordService.initiatePasswordReset("unknown", null, "cashbook"))
        .thenReturn(PasswordResetResult.UserNotFound)

      val request = ResetPasswordRequest(username = "unknown", application = "cashbook")
      val response = resetPasswordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `AUTH-047 returns 400 when account is locked`() {
      whenever(passwordService.initiatePasswordReset("testuser", null, "cashbook"))
        .thenReturn(PasswordResetResult.AccountLocked)

      val request = ResetPasswordRequest(username = "testuser", application = "cashbook")
      val response = resetPasswordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `AUTH-048 returns 400 when user has no email`() {
      whenever(passwordService.initiatePasswordReset("testuser", null, "cashbook"))
        .thenReturn(PasswordResetResult.NoEmail)

      val request = ResetPasswordRequest(username = "testuser", application = "cashbook")
      val response = resetPasswordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `AUTH-049 returns 400 when multiple users share same email`() {
      whenever(passwordService.initiatePasswordReset(null, "shared@example.com", "cashbook"))
        .thenReturn(PasswordResetResult.MultipleUsers)

      val request = ResetPasswordRequest(email = "shared@example.com", application = "cashbook")
      val response = resetPasswordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when neither username nor email provided`() {
      val request = ResetPasswordRequest(application = "cashbook")
      val response = resetPasswordResource.resetPassword(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }

  @Nested
  @DisplayName("POST /change_password/ authenticated change (AUTH-045)")
  inner class ChangePassword {

    @Test
    fun `AUTH-045 returns 204 on success`() {
      val user = makeUser()
      whenever(passwordService.changePassword("testuser", "oldpass123", "newpass123", "cashbook"))
        .thenReturn(AuthenticatedPasswordChangeResult.Success(user))

      val request = ChangePassword(oldPassword = "oldpass123", newPassword = "newpass123")
      val response = changePasswordResource.changePassword(request, authenticatedCashbookToken())

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `returns 400 with Python error envelope for incorrect old password`() {
      whenever(passwordService.changePassword("testuser", "wrong", "freshpass", "cashbook"))
        .thenReturn(AuthenticatedPasswordChangeResult.IncorrectPassword)

      val request = ChangePassword(oldPassword = "wrong", newPassword = "freshpass")
      val response = changePasswordResource.changePassword(request, authenticatedCashbookToken())

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.body).isEqualTo(
        mapOf(
          "errors" to mapOf(
            "old_password" to listOf("You’ve entered an incorrect password"),
          ),
        ),
      )
    }

    @Test
    fun `returns 400 when old password is missing`() {
      val request = ChangePassword(oldPassword = "", newPassword = "newpass")
      val response = changePasswordResource.changePassword(request, authenticatedCashbookToken())

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.body).isEqualTo(
        mapOf(
          "errors" to mapOf(
            "old_password" to listOf("This field is required."),
          ),
        ),
      )
    }

    @Test
    fun `returns 400 when newPassword is missing`() {
      val request = ChangePassword(oldPassword = "oldpass123", newPassword = "")
      val response = changePasswordResource.changePassword(request, authenticatedCashbookToken())

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.body).isEqualTo(
        mapOf(
          "errors" to mapOf(
            "new_password" to listOf("This field is required."),
          ),
        ),
      )
    }

    @Test
    fun `returns 403 when client application is not allowed`() {
      val request = ChangePassword(oldPassword = "oldpass123", newPassword = "newpass123")

      val response = changePasswordResource.changePassword(request, authenticatedSendMoneyToken())

      assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
  }

  @Nested
  @DisplayName("POST /change_password/{code}/ via reset code")
  inner class ChangePasswordByCode {

    @Test
    fun `returns 204 when password is changed with code`() {
      val token = UUID.randomUUID()
      val user = makeUser()
      whenever(passwordService.changePasswordByToken(token, "newpass123"))
        .thenReturn(PasswordChangeResult.Success(user))

      val request = ChangePasswordByTokenRequest(newPassword = "newpass123")
      val response = changePasswordResource.changePasswordByCode(token.toString(), request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `returns 400 with Python error envelope when new_password is missing`() {
      val response = changePasswordResource.changePasswordByCode(UUID.randomUUID().toString(), ChangePasswordByTokenRequest(newPassword = null))

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(response.body).isEqualTo(
        mapOf(
          "errors" to mapOf(
            "new_password" to listOf("This field is required."),
          ),
        ),
      )
    }

    @Test
    fun `returns 404 when code is not a UUID`() {
      val response = changePasswordResource.changePasswordByCode("not-a-uuid", ChangePasswordByTokenRequest(newPassword = "newpass123"))

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
  }
}
