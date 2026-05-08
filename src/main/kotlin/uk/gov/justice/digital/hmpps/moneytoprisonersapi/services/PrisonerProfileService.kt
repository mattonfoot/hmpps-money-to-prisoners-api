package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository

/**
 * Django stores monitoring users on `security_prisonerprofile_monitoring_users`
 * keyed by user-id (integer FK to auth_user). Resolve usernames to user ids when
 * comparing.
 */
@Service
class PrisonerProfileService(
  private val prisonerProfileRepository: PrisonerProfileRepository,
  private val userRepository: AuthUserRepository,
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
      monitoredByUsername != null -> {
        val userId = userRepository.findByUsername(monitoredByUsername)?.id?.toInt()
        if (userId == null) emptyList() else all.filter { it.monitoringUsers.contains(userId) }
      }
      notMonitoredByUsername != null -> {
        val userId = userRepository.findByUsername(notMonitoredByUsername)?.id?.toInt()
        if (userId == null) all else all.filter { !it.monitoringUsers.contains(userId) }
      }
      else -> all
    }
  }

  fun getProfile(id: Long): PrisonerProfile = prisonerProfileRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "PrisonerProfile $id not found") }

  @Transactional
  fun monitor(id: Long, username: String) {
    val profile = getProfile(id)
    val userId = userRepository.findByUsername(username)?.id?.toInt() ?: return
    profile.monitoringUsers.add(userId)
    prisonerProfileRepository.save(profile)
  }

  @Transactional
  fun unmonitor(id: Long, username: String) {
    val profile = getProfile(id)
    val userId = userRepository.findByUsername(username)?.id?.toInt() ?: return
    profile.monitoringUsers.remove(userId)
    prisonerProfileRepository.save(profile)
  }

  fun countMonitoredByUser(username: String): Int {
    val userId = userRepository.findByUsername(username)?.id?.toInt() ?: return 0
    return prisonerProfileRepository.findAll().count { it.monitoringUsers.contains(userId) }
  }
}
