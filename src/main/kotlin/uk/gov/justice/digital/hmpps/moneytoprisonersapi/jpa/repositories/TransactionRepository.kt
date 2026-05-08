package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import java.time.OffsetDateTime

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {

  fun findByCreditId(creditId: Long): Transaction?

  fun findByIdIn(ids: List<Long>): List<Transaction>

  fun findByReceivedAtGreaterThanEqualAndReceivedAtLessThan(
    receivedAtGte: OffsetDateTime,
    receivedAtLt: OffsetDateTime,
  ): List<Transaction>

  fun findByReceivedAtGreaterThanEqual(receivedAt: OffsetDateTime): List<Transaction>

  fun findByReceivedAtLessThan(receivedAt: OffsetDateTime): List<Transaction>
}
