package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile

@Repository
interface SenderProfileRepository : JpaRepository<SenderProfile, Long> {

  // Native query against Django's split monitoring tables:
  //   security_debitcardsenderdetails_monitoring_users(debitcardsenderdetails_id, user_id)
  //   security_bankaccount_monitoring_users(bankaccount_id, user_id)
  // — both link via the per-detail child to the parent SenderProfile.
  @Query(
    nativeQuery = true,
    value = """
      SELECT DISTINCT c.id
      FROM credit_credit c
      WHERE c.sender_profile_id IS NOT NULL AND (
        c.sender_profile_id IN (
          SELECT d.sender_id
          FROM security_debitcardsenderdetails d
          WHERE EXISTS (
            SELECT 1 FROM security_debitcardsenderdetails_monitoring_users m
            WHERE m.debitcardsenderdetails_id = d.id
          )
        )
        OR c.sender_profile_id IN (
          SELECT b.sender_id
          FROM security_banktransfersenderdetails b
          WHERE b.sender_bank_account_id IS NOT NULL AND EXISTS (
            SELECT 1 FROM security_bankaccount_monitoring_users m
            WHERE m.bankaccount_id = b.sender_bank_account_id
          )
        )
      )
    """,
  )
  fun findCreditIdsWithMonitoredSenderProfiles(): Set<Long>

  @Query(
    "SELECT DISTINCT sp FROM SecuritySenderprofile sp JOIN sp.credits c JOIN c.transaction t " +
      "WHERE t.senderSortCode = :sortCode AND t.senderAccountNumber = :accountNumber",
  )
  fun findBySenderBankAccount(sortCode: String, accountNumber: String): List<SenderProfile>

  @Query(
    "SELECT DISTINCT sp FROM SecuritySenderprofile sp JOIN sp.credits c JOIN c.payment p " +
      "WHERE p.cardNumberFirstDigits = :firstDigits AND p.cardNumberLastDigits = :lastDigits " +
      "AND p.cardExpiryDate = :expiryDate",
  )
  fun findBySenderCard(firstDigits: String, lastDigits: String, expiryDate: String): List<SenderProfile>
}
