package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile

@Repository
interface SenderProfileRepository : JpaRepository<SenderProfile, Long> {

  @Query("SELECT DISTINCT c.id FROM SenderProfile sp JOIN sp.credits c WHERE sp.monitoringUsers IS NOT EMPTY")
  fun findCreditIdsWithMonitoredSenderProfiles(): Set<Long>

  @Query(
    "SELECT DISTINCT sp FROM SenderProfile sp JOIN sp.credits c JOIN c.transaction t " +
      "WHERE t.senderSortCode = :sortCode AND t.senderAccountNumber = :accountNumber",
  )
  fun findBySenderBankAccount(sortCode: String, accountNumber: String): List<SenderProfile>

  @Query(
    "SELECT DISTINCT sp FROM SenderProfile sp JOIN sp.credits c JOIN c.payment p " +
      "WHERE p.cardNumberFirstDigits = :firstDigits AND p.cardNumberLastDigits = :lastDigits " +
      "AND p.cardExpiryDate = :expiryDate",
  )
  fun findBySenderCard(firstDigits: String, lastDigits: String, expiryDate: String): List<SenderProfile>
}
