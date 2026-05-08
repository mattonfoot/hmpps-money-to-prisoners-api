package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile

@Repository
interface PrisonerProfileRepository : JpaRepository<PrisonerProfile, Long> {

  @Query("SELECT DISTINCT c.id FROM SecurityPrisonerprofile pp JOIN pp.credits c WHERE pp.monitoringUsers IS NOT EMPTY")
  fun findCreditIdsWithMonitoredPrisonerProfiles(): Set<Long>

  fun findByPrisonerNumber(prisonerNumber: String): List<PrisonerProfile>

  // Django models the credits relation as `credit_credit.prisoner_profile_id`
  // (FK on credit), not via a junction table. Count distinct senders on the
  // credits assigned to this prisoner profile.
  @Query(
    """
    SELECT COUNT(DISTINCT c.sender_profile_id)
    FROM credit_credit c
    WHERE c.prisoner_profile_id = :profileId
      AND c.sender_profile_id IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun countSendersForProfile(profileId: Long): Int
}
