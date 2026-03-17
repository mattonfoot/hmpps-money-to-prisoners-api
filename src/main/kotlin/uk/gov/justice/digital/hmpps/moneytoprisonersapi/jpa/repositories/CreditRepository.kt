package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import java.time.LocalDateTime

@Repository
interface CreditRepository : JpaRepository<Credit, Long> {
  fun findByResolutionNotIn(resolutions: List<CreditResolution>): List<Credit>
  fun findByResolution(resolution: CreditResolution): List<Credit>
  fun findByPrison(prison: String): List<Credit>
  fun findByPrisonIsNull(): List<Credit>
  fun findByBlocked(blocked: Boolean): List<Credit>
  fun findByReviewed(reviewed: Boolean): List<Credit>
  fun findByOwner(owner: String): List<Credit>
  fun findByReceivedAtGreaterThanEqualAndReceivedAtBefore(from: LocalDateTime, to: LocalDateTime): List<Credit>
  fun findByPrisonerNumber(prisonerNumber: String): List<Credit>
  fun existsByPrisonerNumberAndResolution(prisonerNumber: String, resolution: CreditResolution): Boolean
}
