package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerBalance

@Repository
interface PrisonerBalanceRepository : JpaRepository<PrisonerBalance, Long> {
  fun findByPrisonerNumber(prisonerNumber: String): List<PrisonerBalance>
}
