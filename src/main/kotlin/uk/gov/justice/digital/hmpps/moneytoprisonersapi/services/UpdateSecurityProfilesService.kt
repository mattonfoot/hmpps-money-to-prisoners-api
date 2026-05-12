package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.RecipientProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.RecipientProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

/**
 * Mirrors the Django `update_security_profiles` management command
 * (`mtp_api/apps/security/management/commands/update_security_profiles.py`).
 *
 * SEC-080..082: recalculate sender profile totals from CREDITED credits and mark them counted.
 * SEC-085..087: recalculate prisoner profile credit + disbursement totals.
 * SEC-090     : recalculate recipient profile disbursement totals (SENT only).
 * SEC-095     : full pass across every profile.
 */
@Service
class UpdateSecurityProfilesService(
  private val senderProfileRepository: SenderProfileRepository,
  private val prisonerProfileRepository: PrisonerProfileRepository,
  private val recipientProfileRepository: RecipientProfileRepository,
  private val disbursementRepository: DisbursementRepository,
) {

  @Transactional
  fun recalculateSenderProfileTotals(profile: SenderProfile): SenderProfile {
    val creditedCreditValue = CreditResolution.CREDITED.value
    val credited = profile.credits.filter { it.resolution == creditedCreditValue }
    profile.creditCount = credited.size.toLong()
    profile.creditTotal = credited.sumOf { it.amount }
    credited.forEach { it.isCountedInSenderProfileTotal = true }
    return senderProfileRepository.save(profile)
  }

  @Transactional
  fun recalculatePrisonerProfileTotals(profile: PrisonerProfile): PrisonerProfile {
    val creditedCreditValue = CreditResolution.CREDITED.value
    val credited = profile.credits.filter { it.resolution == creditedCreditValue }
    profile.creditCount = credited.size.toLong()
    profile.creditTotal = credited.sumOf { it.amount }
    credited.forEach { it.isCountedInPrisonerProfileTotal = true }

    val sentDisbursementValue = DisbursementResolution.SENT.value
    val sent = disbursementRepository.findByPrisonerProfile(profile)
      .filter { it.resolution == sentDisbursementValue }
    profile.disbursementCount = sent.size.toLong()
    profile.disbursementTotal = sent.sumOf { it.amount.toLong() }
    return prisonerProfileRepository.save(profile)
  }

  @Transactional
  fun recalculateRecipientProfileTotals(profile: RecipientProfile): RecipientProfile {
    val sentDisbursementValue = DisbursementResolution.SENT.value
    val sent = profile.disbursements.filter { it.resolution == sentDisbursementValue }
    profile.disbursementCount = sent.size.toLong()
    profile.disbursementTotal = sent.sumOf { it.amount.toLong() }
    return recipientProfileRepository.save(profile)
  }

  @Transactional
  fun recalculateAllProfileTotals() {
    senderProfileRepository.findAll().forEach { recalculateSenderProfileTotals(it) }
    prisonerProfileRepository.findAll().forEach { recalculatePrisonerProfileTotals(it) }
    recipientProfileRepository.findAll().forEach { recalculateRecipientProfileTotals(it) }
  }
}
