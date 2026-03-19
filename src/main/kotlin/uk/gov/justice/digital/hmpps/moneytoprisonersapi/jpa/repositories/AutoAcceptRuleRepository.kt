package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AutoAcceptRule
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile

@Repository
interface AutoAcceptRuleRepository : JpaRepository<AutoAcceptRule, Long> {

  fun findBySenderProfileAndPrisonerProfile(
    senderProfile: SenderProfile,
    prisonerProfile: PrisonerProfile,
  ): AutoAcceptRule?

  fun findBySenderProfileId(senderProfileId: Long): List<AutoAcceptRule>

  fun findByPrisonerProfileId(prisonerProfileId: Long): List<AutoAcceptRule>
}
