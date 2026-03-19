package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile

@Repository
interface PrisonerProfileRepository : JpaRepository<PrisonerProfile, Long> {

  @Query("SELECT DISTINCT c.id FROM PrisonerProfile pp JOIN pp.credits c WHERE pp.monitoringUsers IS NOT EMPTY")
  fun findCreditIdsWithMonitoredPrisonerProfiles(): Set<Long>

  fun findByPrisonerNumber(prisonerNumber: String): List<PrisonerProfile>
}
