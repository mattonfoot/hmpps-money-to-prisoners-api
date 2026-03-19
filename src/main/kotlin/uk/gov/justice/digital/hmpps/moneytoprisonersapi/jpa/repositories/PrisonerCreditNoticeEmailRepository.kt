package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerCreditNoticeEmail

@Repository
interface PrisonerCreditNoticeEmailRepository : JpaRepository<PrisonerCreditNoticeEmail, String>
