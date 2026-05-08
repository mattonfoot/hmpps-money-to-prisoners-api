package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile

@Repository
interface AutoAcceptRuleRepository : JpaRepository<AutoAcceptRule, Long> {

  // Django models the rule keyed off `debit_card_sender_details_id`, not the
  // sender profile directly. Walk through the per-detail child to find the
  // matching rule for a (sender_profile, prisoner_profile) pair.
  @Query(
    """
    SELECT r FROM SecurityCheckautoacceptrule r
    WHERE r.prisonerProfile = :prisonerProfile
      AND r.debitCardSenderDetails.sender = :senderProfile
    """,
  )
  fun findBySenderProfileAndPrisonerProfile(
    senderProfile: SenderProfile,
    prisonerProfile: PrisonerProfile,
  ): AutoAcceptRule?

  @Query(
    """
    SELECT r FROM SecurityCheckautoacceptrule r
    WHERE r.debitCardSenderDetails.sender.id = :senderProfileId
    """,
  )
  fun findBySenderProfileId(senderProfileId: Long): List<AutoAcceptRule>

  @Query("SELECT r FROM SecurityCheckautoacceptrule r WHERE r.prisonerProfile.id = :prisonerProfileId")
  fun findByPrisonerProfileId(prisonerProfileId: Long): List<AutoAcceptRule>
}
