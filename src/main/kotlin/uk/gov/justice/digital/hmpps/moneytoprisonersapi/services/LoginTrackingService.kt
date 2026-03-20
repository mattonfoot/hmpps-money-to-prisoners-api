package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FailedLoginAttempt
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUserLogin
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.FailedLoginAttemptRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserLoginRepository
import java.time.LocalDateTime

/** Usernames excluded from all login tracking (AUTH-006). */
val SERVICE_ACCOUNT_USERNAMES: Set<String> = setOf("send-money", "bank-admin", "transaction-uploader")

/** Number of failed attempts within [LOCKOUT_PERIOD_MINUTES] that triggers lockout (AUTH-003). */
const val LOCKOUT_COUNT: Int = 5

/** Rolling window in minutes in which failed attempts are counted (AUTH-003). */
const val LOCKOUT_PERIOD_MINUTES: Long = 30

@Service
class LoginTrackingService(
  private val failedLoginAttemptRepository: FailedLoginAttemptRepository,
  private val mtpUserLoginRepository: MtpUserLoginRepository,
) {

  /**
   * AUTH-002: Records a failed login attempt for [user] in [application].
   * AUTH-006: Silently skips service accounts.
   */
  @Transactional
  fun recordFailedLogin(user: MtpUser, application: String) {
    if (user.username in SERVICE_ACCOUNT_USERNAMES) return
    failedLoginAttemptRepository.save(
      FailedLoginAttempt(
        user = user,
        application = application,
        attemptedAt = LocalDateTime.now(),
      ),
    )
  }

  /**
   * AUTH-003: Returns true if [user] has [LOCKOUT_COUNT] or more failed attempts in [application]
   * within the last [LOCKOUT_PERIOD_MINUTES] minutes.
   */
  @Transactional(readOnly = true)
  fun isLocked(user: MtpUser, application: String): Boolean {
    val since = LocalDateTime.now().minusMinutes(LOCKOUT_PERIOD_MINUTES)
    val count = failedLoginAttemptRepository.countByUserAndApplicationAndAttemptedAtAfter(user, application, since)
    return count >= LOCKOUT_COUNT
  }

  /**
   * AUTH-005: Records a successful login for [user] in [application].
   * AUTH-006: Silently skips service accounts.
   */
  @Transactional
  fun recordLogin(user: MtpUser, application: String) {
    if (user.username in SERVICE_ACCOUNT_USERNAMES) return
    mtpUserLoginRepository.save(
      MtpUserLogin(
        user = user,
        application = application,
        loggedInAt = LocalDateTime.now(),
      ),
    )
  }

  /**
   * AUTH-017: Unlocks a user by clearing all their failed login attempts.
   */
  @Transactional
  fun unlockUser(user: MtpUser) {
    failedLoginAttemptRepository.deleteByUser(user)
  }

  /**
   * AUTH-042: Clears failed attempts for [user] in [application] after a successful password change.
   */
  @Transactional
  fun clearFailedAttempts(user: MtpUser, application: String) {
    failedLoginAttemptRepository.deleteByUserAndApplication(user, application)
  }
}
