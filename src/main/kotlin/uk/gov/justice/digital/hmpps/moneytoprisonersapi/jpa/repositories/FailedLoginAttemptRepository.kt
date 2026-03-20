package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.FailedLoginAttempt
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import java.time.LocalDateTime

interface FailedLoginAttemptRepository : JpaRepository<FailedLoginAttempt, Long> {

  fun countByUserAndApplicationAndAttemptedAtAfter(
    user: MtpUser,
    application: String,
    since: LocalDateTime,
  ): Long

  @Modifying
  @Query("DELETE FROM FailedLoginAttempt f WHERE f.user = :user AND f.application = :application")
  fun deleteByUserAndApplication(user: MtpUser, application: String)

  @Modifying
  @Query("DELETE FROM FailedLoginAttempt f WHERE f.user = :user")
  fun deleteByUser(user: MtpUser)
}
