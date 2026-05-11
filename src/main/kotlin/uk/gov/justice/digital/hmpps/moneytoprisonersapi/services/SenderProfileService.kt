package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SecurityDebitcardsenderdetailsMonitoringUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

/**
 * Django models monitoring users on the per-detail children of a sender profile
 * (`security_debitcardsenderdetails_monitoring_users`), not on the parent
 * profile itself. The `monitor`/`unmonitor`/`listProfiles(monitored…)` paths
 * walk through the detail children to mirror that shape.
 */
@Service
class SenderProfileService(
  private val senderProfileRepository: SenderProfileRepository,
  private val monitoringUserRepository: SecurityDebitcardsenderdetailsMonitoringUserRepository,
  private val authUserRepository: AuthUserRepository,
) {

  fun listProfiles(
    monitoredByUsername: String? = null,
    notMonitoredByUsername: String? = null,
  ): List<SenderProfile> {
    val all = senderProfileRepository.findAll()
    val username = monitoredByUsername ?: notMonitoredByUsername ?: return all
    val user = authUserRepository.findByUsername(username) ?: return if (monitoredByUsername != null) emptyList() else all
    val monitoredIds = monitoringUserRepository.findSenderProfileIdsMonitoredBy(user.id!!)
    return when {
      monitoredByUsername != null -> all.filter { it.id in monitoredIds }
      else -> all.filterNot { it.id in monitoredIds }
    }
  }

  fun getProfile(id: Long): SenderProfile = senderProfileRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SenderProfile $id not found") }

  fun isMonitoredBy(senderProfileId: Long, username: String): Boolean {
    val user = authUserRepository.findByUsername(username) ?: return false
    return monitoringUserRepository.isSenderProfileMonitoredBy(senderProfileId, user.id!!)
  }

  @Transactional
  fun monitor(id: Long, username: String) {
    getProfile(id) // 404 if missing
    val user = authUserRepository.findByUsername(username)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User $username not found")
    monitoringUserRepository.monitorSenderProfile(id, user.id!!)
  }

  @Transactional
  fun unmonitor(id: Long, username: String) {
    getProfile(id)
    val user = authUserRepository.findByUsername(username) ?: return
    monitoringUserRepository.unmonitorSenderProfile(id, user.id!!)
  }

  fun countMonitoredByUser(username: String): Int {
    val user = authUserRepository.findByUsername(username) ?: return 0
    return monitoringUserRepository.countSenderProfilesMonitoredBy(user.id!!)
  }
}
