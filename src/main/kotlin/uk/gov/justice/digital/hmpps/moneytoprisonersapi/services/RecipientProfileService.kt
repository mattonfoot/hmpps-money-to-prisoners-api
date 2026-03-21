package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.RecipientProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.RecipientProfileRepository

@Service
class RecipientProfileService(
  private val recipientProfileRepository: RecipientProfileRepository,
  private val disbursementRepository: DisbursementRepository,
) {

  fun listProfiles(
    monitoredByUsername: String? = null,
    notMonitoredByUsername: String? = null,
  ): List<RecipientProfile> {
    val all = recipientProfileRepository.findAll()
    return when {
      monitoredByUsername != null -> all.filter { it.monitoringUsers.contains(monitoredByUsername) }
      notMonitoredByUsername != null -> all.filter { !it.monitoringUsers.contains(notMonitoredByUsername) }
      else -> all
    }
  }

  fun getProfile(id: Long): RecipientProfile = recipientProfileRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "RecipientProfile $id not found") }

  fun getDisbursements(id: Long): List<Disbursement> {
    val profile = getProfile(id)
    return disbursementRepository.findAll().filter { disbursement ->
      profile.sortCode != null &&
        profile.sortCode == disbursement.sortCode &&
        profile.accountNumber != null &&
        profile.accountNumber == disbursement.accountNumber
    }
  }

  @Transactional
  fun monitor(id: Long, username: String) {
    val profile = getProfile(id)
    profile.monitoringUsers.add(username)
    recipientProfileRepository.save(profile)
  }

  @Transactional
  fun unmonitor(id: Long, username: String) {
    val profile = getProfile(id)
    profile.monitoringUsers.remove(username)
    recipientProfileRepository.save(profile)
  }
}
