package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonRemittanceemail

interface PrisonRemittanceEmailRepository : JpaRepository<PrisonRemittanceemail, Long> {
  fun findByPrisonOrderById(prison: Prison): List<PrisonRemittanceemail>
}
