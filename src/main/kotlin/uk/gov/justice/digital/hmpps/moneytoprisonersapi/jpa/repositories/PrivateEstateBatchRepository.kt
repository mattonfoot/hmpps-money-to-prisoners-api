package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import java.time.LocalDate

@Repository
interface PrivateEstateBatchRepository : JpaRepository<PrivateEstateBatch, String> {
  fun findByPrison(prison: String): List<PrivateEstateBatch>
  fun findByDate(date: LocalDate): List<PrivateEstateBatch>
  fun findByDateGreaterThanEqual(date: LocalDate): List<PrivateEstateBatch>
  fun findByDateLessThan(date: LocalDate): List<PrivateEstateBatch>
}
