package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository

@Service
class PrisonerProfileService(
  private val prisonerProfileRepository: PrisonerProfileRepository,
) {

  fun listProfiles(): List<PrisonerProfile> = prisonerProfileRepository.findAll()

  fun getProfile(id: Long): PrisonerProfile = prisonerProfileRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "PrisonerProfile $id not found") }

  @Transactional
  fun monitor(id: Long, username: String) {
    val profile = getProfile(id)
    profile.monitoringUsers.add(username)
    prisonerProfileRepository.save(profile)
  }

  @Transactional
  fun unmonitor(id: Long, username: String) {
    val profile = getProfile(id)
    profile.monitoringUsers.remove(username)
    prisonerProfileRepository.save(profile)
  }

  fun countMonitoredByUser(username: String): Int = prisonerProfileRepository.findAll()
    .count { it.monitoringUsers.contains(username) }
}
