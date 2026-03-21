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

  fun listProfiles(
    monitoredByUsername: String? = null,
    notMonitoredByUsername: String? = null,
    simpleSearch: String? = null,
  ): List<PrisonerProfile> {
    var all = prisonerProfileRepository.findAll()
    if (simpleSearch != null) {
      val terms = simpleSearch.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
      all = all.filter { profile ->
        val searchableText = listOfNotNull(profile.prisonerName, profile.prisonerNumber)
          .joinToString(" ")
          .uppercase()
        terms.all { term -> searchableText.contains(term.uppercase()) }
      }
    }
    return when {
      monitoredByUsername != null -> all.filter { it.monitoringUsers.contains(monitoredByUsername) }
      notMonitoredByUsername != null -> all.filter { !it.monitoringUsers.contains(notMonitoredByUsername) }
      else -> all
    }
  }

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
