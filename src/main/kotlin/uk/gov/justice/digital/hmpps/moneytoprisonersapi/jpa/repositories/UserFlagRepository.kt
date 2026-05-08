package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserFlag

interface UserFlagRepository : JpaRepository<UserFlag, Long> {
  fun findByUser(user: MtpUser): List<UserFlag>
  fun findByUserAndName(user: MtpUser, name: String): UserFlag?
  fun existsByUserAndName(user: MtpUser, name: String): Boolean
}
