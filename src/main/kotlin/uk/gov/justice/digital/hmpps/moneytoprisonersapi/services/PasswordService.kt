package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PasswordResetToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PasswordResetTokenRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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

/** Result type for authenticated password changes via old_password/new_password. */
sealed class AuthenticatedPasswordChangeResult {
  data class Success(val user: MtpUser) : AuthenticatedPasswordChangeResult()
  object IncorrectPassword : AuthenticatedPasswordChangeResult()
}

/** AUTH-046: usernames that cannot be reset via this endpoint (service / shared accounts). */
internal val IMMUTABLE_USERS: Set<String> = setOf("transaction-uploader", "send-money")

private const val DJANGO_PBKDF2_SHA256 = "pbkdf2_sha256"
private const val DEFAULT_DJANGO_ITERATIONS = 600000
private const val DJANGO_DKLEN_BITS = 256
private const val DJANGO_SALT_LENGTH = 12
private const val DJANGO_SALT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

private object DjangoPasswordHasher {
  private val secureRandom = SecureRandom()

  fun matches(rawPassword: String, encodedPassword: String): Boolean {
    val parts = encodedPassword.split('$')
    if (parts.size != 4 || parts[0] != DJANGO_PBKDF2_SHA256) return false
    val iterations = parts[1].toIntOrNull() ?: return false
    val salt = parts[2]
    val expected = Base64.getDecoder().decode(parts[3])
    val actual = Base64.getDecoder().decode(pbkdf2(rawPassword, salt, iterations))
    return MessageDigest.isEqual(actual, expected)
  }

  fun encode(rawPassword: String, referenceHash: String? = null): String {
    val iterations = parseIterations(referenceHash) ?: DEFAULT_DJANGO_ITERATIONS
    val salt = randomSalt()
    val hash = pbkdf2(rawPassword, salt, iterations)
    return listOf(DJANGO_PBKDF2_SHA256, iterations.toString(), salt, hash).joinToString("$")
  }

  private fun parseIterations(referenceHash: String?): Int? {
    val parts = referenceHash?.split('$') ?: return null
    if (parts.size != 4 || parts[0] != DJANGO_PBKDF2_SHA256) return null
    return parts[1].toIntOrNull()
  }

  private fun randomSalt(): String = buildString(DJANGO_SALT_LENGTH) {
    repeat(DJANGO_SALT_LENGTH) {
      append(DJANGO_SALT_CHARS[secureRandom.nextInt(DJANGO_SALT_CHARS.length)])
    }
  }

  private fun pbkdf2(password: String, salt: String, iterations: Int): String {
    val keySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), iterations, DJANGO_DKLEN_BITS)
    val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec)
    return Base64.getEncoder().encodeToString(secret.encoded)
  }
}

@Service
class PasswordService(
  private val mtpUserRepository: MtpUserRepository,
  private val passwordResetTokenRepository: PasswordResetTokenRepository,
  private val loginTrackingService: LoginTrackingService,
) {

  /** Token-id generator. Tests can override (e.g. for determinism). */
  internal var tokenIdSupplier: () -> UUID = { UUID.randomUUID() }

  /**
   * AUTH-043: Initiates a password reset for the user identified by [username] or [email].
   * AUTH-046: Immutable/service accounts (send-money, transaction-uploader) are
   * treated as not-found to mirror Python's ResetPasswordView behaviour.
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
    if (user.username in IMMUTABLE_USERS) return PasswordResetResult.UserNotFound
    if (loginTrackingService.isLocked(user, application)) return PasswordResetResult.AccountLocked
    if (user.email.isBlank()) return PasswordResetResult.NoEmail

    val resetToken = passwordResetTokenRepository.save(
      PasswordResetToken().apply {
        this.id = tokenIdSupplier()
        this.user = user
      },
    )
    return PasswordResetResult.TokenCreated(resetToken)
  }

  /**
   * AUTH-045: Changes an authenticated user's password using their current password.
   */
  @Transactional
  fun changePassword(
    username: String,
    oldPassword: String,
    newPassword: String,
    application: String,
  ): AuthenticatedPasswordChangeResult {
    val user = mtpUserRepository.findByUsernameIgnoreCase(username) ?: return AuthenticatedPasswordChangeResult.IncorrectPassword
    if (loginTrackingService.isLocked(user, application)) {
      return AuthenticatedPasswordChangeResult.IncorrectPassword
    }
    if (!DjangoPasswordHasher.matches(oldPassword, user.password)) {
      loginTrackingService.recordFailedLogin(user, application)
      return AuthenticatedPasswordChangeResult.IncorrectPassword
    }
    loginTrackingService.clearFailedAttempts(user, application)
    user.password = DjangoPasswordHasher.encode(newPassword, user.password)
    mtpUserRepository.save(user)
    return AuthenticatedPasswordChangeResult.Success(user)
  }

  /**
   * AUTH-045: Changes the user's password using a one-time reset [token].
   * AUTH-042: Clears failed login attempts on success.
   */
  @Transactional
  fun changePasswordByToken(token: UUID, newPassword: String): PasswordChangeResult {
    val resetToken = passwordResetTokenRepository.findById(token).orElse(null)
      ?: return PasswordChangeResult.InvalidToken
    val owner = resetToken.user ?: return PasswordChangeResult.InvalidToken
    owner.password = DjangoPasswordHasher.encode(newPassword, owner.password)
    mtpUserRepository.save(owner)
    loginTrackingService.clearFailedAttempts(owner, application = null)
    return PasswordChangeResult.Success(owner)
  }

  /**
   * AUTH-041: Increments the failed login attempt count when a password change is denied.
   */
  fun recordFailedPasswordChange(user: MtpUser, application: String) {
    loginTrackingService.recordFailedLogin(user, application)
  }
}
