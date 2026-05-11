package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityDebitcardsenderdetailsMonitoringUser

@Repository
interface SecurityDebitcardsenderdetailsMonitoringUserRepository :
  JpaRepository<SecurityDebitcardsenderdetailsMonitoringUser, Long> {

  /** Returns sender profile ids whose any debit-card detail is monitored by [userId]. */
  @Query(
    nativeQuery = true,
    value = """
      SELECT DISTINCT d.sender_id
      FROM security_debitcardsenderdetails_monitoring_users m
      JOIN security_debitcardsenderdetails d ON d.id = m.debitcardsenderdetails_id
      WHERE m.user_id = :userId
    """,
  )
  fun findSenderProfileIdsMonitoredBy(@Param("userId") userId: Long): Set<Long>

  /** Adds a user as monitor on every debit-card detail of [senderProfileId]. */
  @Transactional
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
      INSERT INTO security_debitcardsenderdetails_monitoring_users
        (debitcardsenderdetails_id, user_id)
      SELECT d.id, :userId
      FROM security_debitcardsenderdetails d
      WHERE d.sender_id = :senderProfileId
        AND NOT EXISTS (
          SELECT 1 FROM security_debitcardsenderdetails_monitoring_users m
          WHERE m.debitcardsenderdetails_id = d.id AND m.user_id = :userId
        )
    """,
  )
  fun monitorSenderProfile(
    @Param("senderProfileId") senderProfileId: Long,
    @Param("userId") userId: Long,
  )

  /** Removes a user from monitoring on every debit-card detail of [senderProfileId]. */
  @Transactional
  @Modifying
  @Query(
    nativeQuery = true,
    value = """
      DELETE FROM security_debitcardsenderdetails_monitoring_users m
      USING security_debitcardsenderdetails d
      WHERE m.debitcardsenderdetails_id = d.id
        AND d.sender_id = :senderProfileId
        AND m.user_id = :userId
    """,
  )
  fun unmonitorSenderProfile(
    @Param("senderProfileId") senderProfileId: Long,
    @Param("userId") userId: Long,
  )

  /** Returns true if [userId] monitors ANY detail child of [senderProfileId]. */
  @Query(
    nativeQuery = true,
    value = """
      SELECT EXISTS (
        SELECT 1
        FROM security_debitcardsenderdetails_monitoring_users m
        JOIN security_debitcardsenderdetails d ON d.id = m.debitcardsenderdetails_id
        WHERE d.sender_id = :senderProfileId AND m.user_id = :userId
      )
    """,
  )
  fun isSenderProfileMonitoredBy(
    @Param("senderProfileId") senderProfileId: Long,
    @Param("userId") userId: Long,
  ): Boolean

  /** Count of sender profiles monitored by user. */
  @Query(
    nativeQuery = true,
    value = """
      SELECT COUNT(DISTINCT d.sender_id)
      FROM security_debitcardsenderdetails_monitoring_users m
      JOIN security_debitcardsenderdetails d ON d.id = m.debitcardsenderdetails_id
      WHERE m.user_id = :userId
    """,
  )
  fun countSenderProfilesMonitoredBy(@Param("userId") userId: Long): Int
}
