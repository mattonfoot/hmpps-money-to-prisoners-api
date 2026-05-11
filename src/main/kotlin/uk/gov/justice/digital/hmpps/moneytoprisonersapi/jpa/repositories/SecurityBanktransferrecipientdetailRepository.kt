package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityBanktransferrecipientdetail
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityRecipientprofile

/**
 * Repository for the per-detail child of a recipient profile.
 * Recipient monitoring lives on the bank account underneath this detail —
 * specifically `security_bankaccount_monitoring_users`. The native queries
 * here mirror Django's data layout.
 */
@Repository
interface SecurityBanktransferrecipientdetailRepository : JpaRepository<SecurityBanktransferrecipientdetail, Long> {

  fun findByRecipient(recipient: SecurityRecipientprofile): List<SecurityBanktransferrecipientdetail>

  @Query(
    nativeQuery = true,
    value = """
      SELECT DISTINCT r.recipient_id
      FROM security_banktransferrecipientdetails r
      JOIN security_bankaccount_monitoring_users m
        ON m.bankaccount_id = r.recipient_bank_account_id
      WHERE m.user_id = :userId
    """,
  )
  fun findRecipientProfileIdsMonitoredBy(@Param("userId") userId: Long): Set<Long>

  @Transactional
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
      INSERT INTO security_bankaccount_monitoring_users (bankaccount_id, user_id)
      SELECT r.recipient_bank_account_id, :userId
      FROM security_banktransferrecipientdetails r
      WHERE r.recipient_id = :recipientProfileId
        AND NOT EXISTS (
          SELECT 1 FROM security_bankaccount_monitoring_users m
          WHERE m.bankaccount_id = r.recipient_bank_account_id AND m.user_id = :userId
        )
    """,
  )
  fun monitorRecipientProfile(
    @Param("recipientProfileId") recipientProfileId: Long,
    @Param("userId") userId: Long,
  )

  @Transactional
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
      DELETE FROM security_bankaccount_monitoring_users m
      USING security_banktransferrecipientdetails r
      WHERE m.bankaccount_id = r.recipient_bank_account_id
        AND r.recipient_id = :recipientProfileId
        AND m.user_id = :userId
    """,
  )
  fun unmonitorRecipientProfile(
    @Param("recipientProfileId") recipientProfileId: Long,
    @Param("userId") userId: Long,
  )

  @Query(
    nativeQuery = true,
    value = """
      SELECT EXISTS (
        SELECT 1
        FROM security_banktransferrecipientdetails r
        JOIN security_bankaccount_monitoring_users m
          ON m.bankaccount_id = r.recipient_bank_account_id
        WHERE r.recipient_id = :recipientProfileId AND m.user_id = :userId
      )
    """,
  )
  fun isRecipientProfileMonitoredBy(
    @Param("recipientProfileId") recipientProfileId: Long,
    @Param("userId") userId: Long,
  ): Boolean
}
