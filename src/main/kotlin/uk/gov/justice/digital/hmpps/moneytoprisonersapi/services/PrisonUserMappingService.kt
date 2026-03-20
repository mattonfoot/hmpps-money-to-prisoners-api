package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository

@Service
class PrisonUserMappingService(
  private val mtpUserRepository: MtpUserRepository,
) {

  /**
   * AUTH-051: Returns all prisons mapped to [user].
   */
  fun getPrisonsForUser(user: MtpUser): Set<Prison> = user.prisons.toSet()

  /**
   * AUTH-050: Replaces the prison mapping for [user] with [prisons].
   */
  @Transactional
  fun assignPrisons(user: MtpUser, prisons: Set<Prison>) {
    user.prisons = prisons.toMutableSet()
    mtpUserRepository.save(user)
  }

  /**
   * AUTH-052: Copies the prison mapping from [source] user to [target] user,
   * overriding any existing assignments on [target].
   */
  @Transactional
  fun copyPrisonMapping(source: MtpUser, target: MtpUser) {
    target.prisons = source.prisons.toMutableSet()
    mtpUserRepository.save(target)
  }
}
