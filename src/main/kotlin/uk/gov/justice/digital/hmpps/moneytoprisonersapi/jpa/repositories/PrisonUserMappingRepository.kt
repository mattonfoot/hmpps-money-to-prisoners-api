package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpAuthPrisonusermapping
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser

interface PrisonUserMappingRepository : JpaRepository<MtpAuthPrisonusermapping, Long> {
  fun findByUser(user: MtpUser): MtpAuthPrisonusermapping?
}
