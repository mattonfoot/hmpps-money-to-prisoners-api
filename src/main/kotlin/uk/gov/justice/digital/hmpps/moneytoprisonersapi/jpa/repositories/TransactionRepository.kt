package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {
  fun findByCreditId(creditId: Long): Transaction?
}
