package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpAuthPrisonusermapping
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonUserMappingRepository

/**
 * Django models user-to-prison assignment via mtp_auth_prisonusermapping (one
 * row per user) plus the mtp_auth_prisonusermapping_prisons join table. The
 * legacy Kotlin `user.prisons = …` assignment goes through this mapping row.
 */
@Service
class PrisonUserMappingService(
  private val mtpUserRepository: MtpUserRepository,
  private val mappingRepository: PrisonUserMappingRepository,
) {

  /** AUTH-051: Returns all prisons mapped to [user]. */
  fun getPrisonsForUser(user: MtpUser): Set<Prison> = user.prisonUserMapping?.prisons ?: emptySet()

  private fun mappingFor(user: MtpUser): MtpAuthPrisonusermapping = user.prisonUserMapping
    ?: MtpAuthPrisonusermapping().also {
      it.user = user
      // Wire the back-reference too so callers reading `user.prisonUserMapping`
      // (or `user.prisons`) immediately after see the new mapping.
      user.prisonUserMapping = it
    }

  /** AUTH-050: Replaces the prison mapping for [user] with [prisons]. */
  @Transactional
  fun assignPrisons(user: MtpUser, prisons: Set<Prison>) {
    val mapping = mappingFor(user)
    mapping.prisons.clear()
    mapping.prisons.addAll(prisons)
    mappingRepository.save(mapping)
    mtpUserRepository.save(user)
  }

  /** AUTH-052: Copies the prison mapping from [source] user to [target] user. */
  @Transactional
  fun copyPrisonMapping(source: MtpUser, target: MtpUser) {
    val targetMapping = mappingFor(target)
    targetMapping.prisons.clear()
    targetMapping.prisons.addAll(source.prisonUserMapping?.prisons ?: emptySet())
    mappingRepository.save(targetMapping)
    mtpUserRepository.save(target)
  }
}
