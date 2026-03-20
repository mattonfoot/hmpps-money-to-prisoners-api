package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser

interface MtpUserRepository :
  JpaRepository<MtpUser, Long>,
  JpaSpecificationExecutor<MtpUser> {
  fun findByUsernameIgnoreCase(username: String): MtpUser?
  fun existsByUsernameIgnoreCase(username: String): Boolean
  fun existsByEmailIgnoreCase(email: String): Boolean
}
