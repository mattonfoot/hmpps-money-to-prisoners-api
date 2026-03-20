package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PasswordResetToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PasswordResetTokenRepository
import java.util.UUID

/** Result type for initiating a password reset (AUTH-043 to AUTH-049). */
sealed class PasswordResetResult {
  data class TokenCreated(val token: PasswordResetToken) : PasswordResetResult()
  object UserNotFound : PasswordResetResult()
  object AccountLocked : PasswordResetResult()
  object NoEmail : PasswordResetResult()
  object MultipleUsers : PasswordResetResult()
}

/** Result type for completing a password change via token (AUTH-045). */
sealed class PasswordChangeResult {
  data class Success(val user: MtpUser) : PasswordChangeResult()
  object InvalidToken : PasswordChangeResult()
}

@Service
class PasswordService(
  private val mtpUserRepository: MtpUserRepository,
  private val passwordResetTokenRepository: PasswordResetTokenRepository,
  private val loginTrackingService: LoginTrackingService,
) {

  /**
   * AUTH-043: Initiates a password reset for the user identified by [username] or [email].
   * AUTH-046: Immutable/service accounts are not supported (handled by excluding service accounts from tracking).
   * AUTH-047: Returns AccountLocked if the account is locked.
   * AUTH-048: Returns NoEmail if the user has no email.
   * AUTH-049: Returns MultipleUsers when multiple accounts share the same email.
   */
  @Transactional
  fun initiatePasswordReset(username: String?, email: String?, application: String): PasswordResetResult {
    val candidates: List<MtpUser> = when {
      username != null -> {
        val u = mtpUserRepository.findByUsernameIgnoreCase(username)
        if (u == null) return PasswordResetResult.UserNotFound else listOf(u)
      }
      email != null -> {
        mtpUserRepository.findByEmailIgnoreCase(email).takeIf { it.isNotEmpty() }
          ?: return PasswordResetResult.UserNotFound
      }
      else -> return PasswordResetResult.UserNotFound
    }

    if (candidates.size > 1) return PasswordResetResult.MultipleUsers

    val user = candidates.first()
    if (loginTrackingService.isLocked(user, application)) return PasswordResetResult.AccountLocked
    if (user.email.isBlank()) return PasswordResetResult.NoEmail

    val resetToken = passwordResetTokenRepository.save(
      PasswordResetToken(user = user, application = application),
    )
    return PasswordResetResult.TokenCreated(resetToken)
  }

  /**
   * AUTH-045: Changes the user's password using a one-time reset [token].
   * AUTH-042: Clears failed login attempts on success.
   */
  @Transactional
  fun changePasswordByToken(token: UUID, newPassword: String): PasswordChangeResult {
    val resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
      ?: return PasswordChangeResult.InvalidToken
    resetToken.used = true
    passwordResetTokenRepository.save(resetToken)
    // Password storage is delegated to HMPPS Auth in the production system;
    // here we record the cleared attempts as the side-effect.
    loginTrackingService.clearFailedAttempts(resetToken.user, resetToken.application)
    return PasswordChangeResult.Success(resetToken.user)
  }

  /**
   * AUTH-041: Increments the failed login attempt count when a password change is denied.
   */
  fun recordFailedPasswordChange(user: MtpUser, application: String) {
    loginTrackingService.recordFailedLogin(user, application)
  }
}
