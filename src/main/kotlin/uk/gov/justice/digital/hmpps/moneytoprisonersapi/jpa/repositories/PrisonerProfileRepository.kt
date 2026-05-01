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

  @Query(
    """
    SELECT COUNT(DISTINCT spc.sender_profile_id)
    FROM prisoner_profile_credits ppc
    JOIN sender_profile_credits spc ON spc.credit_id = ppc.credit_id
    WHERE ppc.prisoner_profile_id = :profileId
    """,
    nativeQuery = true,
  )
  fun countSendersForProfile(profileId: Long): Int

}
