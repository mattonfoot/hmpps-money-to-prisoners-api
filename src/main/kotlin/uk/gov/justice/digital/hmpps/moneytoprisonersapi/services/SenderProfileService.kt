package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

/**
 * Django models monitoring users on the per-detail children of a sender profile
 * (e.g. `security_debitcardsenderdetails_monitoring_users`), not on the parent
 * profile itself. The flat `senderProfile.monitoringUsers` view used by the
 * legacy Kotlin code therefore needs to be reconstructed via the detail children.
 *
 * For now `monitor`, `unmonitor` and `listProfiles(monitored…)` are wired as
 * no-ops / pass-throughs so the service compiles and exposes endpoints, but they
 * need re-implementing once the per-detail helpers are in place.
 */
@Service
class SenderProfileService(
  private val senderProfileRepository: SenderProfileRepository,
) {

  fun listProfiles(monitoredByUsername: String? = null, notMonitoredByUsername: String? = null): List<SenderProfile> = senderProfileRepository.findAll()

  fun getProfile(id: Long): SenderProfile = senderProfileRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "SenderProfile $id not found") }

  @Transactional
  fun monitor(id: Long, username: String) {
    @Suppress("UNUSED_VARIABLE")
    val profile = getProfile(id)
    // Walk to security_debitcardsenderdetail / security_banktransfersenderdetail
    // children and update their monitoring_users when re-implemented.
  }

  @Transactional
  fun unmonitor(id: Long, username: String) {
    @Suppress("UNUSED_VARIABLE")
    val profile = getProfile(id)
  }

  fun countMonitoredByUser(username: String): Int = 0
}
