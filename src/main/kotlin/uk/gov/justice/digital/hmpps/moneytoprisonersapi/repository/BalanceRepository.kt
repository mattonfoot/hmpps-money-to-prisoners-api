package uk.gov.justice.digital.hmpps.moneytoprisonersapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.model.Balance

@Repository
interface BalanceRepository : JpaRepository<Balance, Long> {
    fun findAllByOrderByDateDesc(): List<Balance>
}
