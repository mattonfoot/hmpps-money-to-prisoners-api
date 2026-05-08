package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import java.time.LocalDate

@Repository
interface PrivateEstateBatchRepository : JpaRepository<PrivateEstateBatch, Long> {
  fun findByPrison(prison: PrisonPrison): List<PrivateEstateBatch>
  fun findByPrisonAndDate(prison: PrisonPrison, date: LocalDate): PrivateEstateBatch?
}
