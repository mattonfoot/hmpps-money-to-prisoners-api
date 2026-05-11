package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityDebitcardsenderdetail
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecuritySenderprofile

@Repository
interface SecurityDebitcardsenderdetailRepository : JpaRepository<SecurityDebitcardsenderdetail, Long> {
  fun findBySender(sender: SecuritySenderprofile): List<SecurityDebitcardsenderdetail>
}
