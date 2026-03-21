package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

@Service
class SenderProfileService(
  private val senderProfileRepository: SenderProfileRepository,
) {

  fun listProfiles(monitoredByUsername: String? = null, notMonitoredByUsername: String? = null): List<SenderProfile> {
    val all = senderProfileRepository.findAll()
    return when {
      monitoredByUsername != null -> all.filter { it.monitoringUsers.contains(monitoredByUsername) }
      notMonitoredByUsername != null -> all.filter { !it.monitoringUsers.contains(notMonitoredByUsername) }
      else -> all
    }
  }

  fun getProfile(id: Long): SenderProfile = senderProfileRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SenderProfile $id not found") }

  @Transactional
  fun monitor(id: Long, username: String) {
    val profile = getProfile(id)
    profile.monitoringUsers.add(username)
    senderProfileRepository.save(profile)
  }

  @Transactional
  fun unmonitor(id: Long, username: String) {
    val profile = getProfile(id)
    profile.monitoringUsers.remove(username)
    senderProfileRepository.save(profile)
  }

  fun countMonitoredByUser(username: String): Int = senderProfileRepository.findAll()
    .count { it.monitoringUsers.contains(username) }
}
