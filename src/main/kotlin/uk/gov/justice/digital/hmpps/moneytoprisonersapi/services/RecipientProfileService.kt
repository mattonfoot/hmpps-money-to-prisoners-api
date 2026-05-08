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
    // Django doesn't have a `recipient_profile_monitoring_users` table — that
    // concept lives on detail-table children (e.g. security_banktransferrecipientdetail).
    // For now this filter is a no-op. Re-implement when the per-detail
    // monitoring queries are wired in.
    return recipientProfileRepository.findAll()
  }

  fun getProfile(id: Long): RecipientProfile = recipientProfileRepository.findById(id)
    .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "RecipientProfile $id not found") }

  fun getDisbursements(id: Long): List<Disbursement> {
    // The Kotlin profile model used to carry sortCode/accountNumber directly.
    // In Django that lives on security_banktransferrecipientdetail. The lookup
    // here would need to match disbursements via the recipient's bank-transfer
    // detail rows — stubbed until that join is wired in.
    @Suppress("UNUSED_VARIABLE")
    val profile = getProfile(id)
    return emptyList()
  }

  @Transactional
  fun monitor(id: Long, username: String) {
    @Suppress("UNUSED_VARIABLE")
    val profile = getProfile(id)
    // Monitoring users live on the bank-transfer recipient detail child rows.
    // Stubbed until the per-detail monitoring helpers are wired in.
  }

  @Transactional
  fun unmonitor(id: Long, username: String) {
    @Suppress("UNUSED_VARIABLE")
    val profile = getProfile(id)
    // Same as monitor — needs per-detail helpers.
  }
}
