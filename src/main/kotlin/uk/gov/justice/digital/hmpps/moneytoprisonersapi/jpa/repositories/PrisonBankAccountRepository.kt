package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrisonbankaccount

interface PrisonBankAccountRepository : JpaRepository<PrisonPrisonbankaccount, Long> {
  fun findByPrison(prison: Prison): PrisonPrisonbankaccount?
}
