package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.RecipientProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityBanktransferrecipientdetail
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.RecipientProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SecurityBanktransferrecipientdetailRepository

@Service
class RecipientProfileService(
  private val recipientProfileRepository: RecipientProfileRepository,
  private val disbursementRepository: DisbursementRepository,
  private val recipientDetailRepository: SecurityBanktransferrecipientdetailRepository,
  private val authUserRepository: AuthUserRepository,
) {

  fun listProfiles(
    monitoredByUsername: String? = null,
    notMonitoredByUsername: String? = null,
  ): List<RecipientProfile> {
    val all = recipientProfileRepository.findAll()
    val username = monitoredByUsername ?: notMonitoredByUsername ?: return all
    val user = authUserRepository.findByUsername(username) ?: return if (monitoredByUsername != null) emptyList() else all
    val monitoredIds = recipientDetailRepository.findRecipientProfileIdsMonitoredBy(user.id!!)
    return when {
      monitoredByUsername != null -> all.filter { it.id in monitoredIds }
      else -> all.filterNot { it.id in monitoredIds }
    }
  }

  fun getProfile(id: Long): RecipientProfile = recipientProfileRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "RecipientProfile $id not found") }

  fun getDetails(id: Long): List<SecurityBanktransferrecipientdetail> = recipientDetailRepository.findByRecipient(getProfile(id))

  fun isMonitoredBy(recipientProfileId: Long, username: String): Boolean {
    val user = authUserRepository.findByUsername(username) ?: return false
    return recipientDetailRepository.isRecipientProfileMonitoredBy(recipientProfileId, user.id!!)
  }

  fun getDisbursements(id: Long): List<Disbursement> {
    val details = getDetails(id)
    if (details.isEmpty()) return emptyList()
    // Disbursements match on (sortCode, accountNumber). Walk through each
    // detail's bank account and gather matching disbursements.
    val keys = details.mapNotNull { it.recipientBankAccount }
      .map { it.sortCode to it.accountNumber }
      .toSet()
    return disbursementRepository.findAll().filter { d ->
      (d.sortCode to d.accountNumber) in keys
    }
  }

  @Transactional
  fun monitor(id: Long, username: String) {
    getProfile(id) // 404 if missing
    val user = authUserRepository.findByUsername(username)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User $username not found")
    recipientDetailRepository.monitorRecipientProfile(id, user.id!!)
  }

  @Transactional
  fun unmonitor(id: Long, username: String) {
    getProfile(id)
    val user = authUserRepository.findByUsername(username) ?: return
    recipientDetailRepository.unmonitorRecipientProfile(id, user.id!!)
  }
}
