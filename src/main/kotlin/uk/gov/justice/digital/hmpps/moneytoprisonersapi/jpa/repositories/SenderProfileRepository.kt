package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile

@Repository
interface SenderProfileRepository : JpaRepository<SenderProfile, Long> {

  @Query("SELECT DISTINCT c.id FROM SenderProfile sp JOIN sp.credits c WHERE sp.monitoringUsers IS NOT EMPTY")
  fun findCreditIdsWithMonitoredSenderProfiles(): Set<Long>
}
