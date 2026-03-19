package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

/**
 * CRD-210 to CRD-215: Credit Profile Attachment.
 *
 * Attaches credits to sender profiles (grouped by bank account or card details)
 * and prisoner profiles (grouped by prisoner number).
 */
@Service
class AttachProfilesService(
  private val creditRepository: CreditRepository,
  private val senderProfileRepository: SenderProfileRepository,
  private val prisonerProfileRepository: PrisonerProfileRepository,
) {

  @Transactional
  fun attachProfiles(creditIds: List<Long>) {
    if (creditIds.isEmpty()) return

    val credits = creditRepository.findAllById(creditIds)

    for (credit in credits) {
      // Attach to sender profile
      val transaction = credit.transaction
      val payment = credit.payment

      when {
        transaction != null -> {
          val sortCode = transaction.senderSortCode
          val accountNumber = transaction.senderAccountNumber
          if (sortCode != null && accountNumber != null) {
            val existing = senderProfileRepository.findBySenderBankAccount(sortCode, accountNumber)
            val profile = if (existing.isNotEmpty()) existing.first() else SenderProfile()
            profile.credits.add(credit)
            senderProfileRepository.save(profile)
          }
        }
        payment != null -> {
          val firstDigits = payment.cardNumberFirstDigits
          val lastDigits = payment.cardNumberLastDigits
          val expiryDate = payment.cardExpiryDate
          if (firstDigits != null && lastDigits != null && expiryDate != null) {
            val existing = senderProfileRepository.findBySenderCard(firstDigits, lastDigits, expiryDate)
            val profile = if (existing.isNotEmpty()) existing.first() else SenderProfile()
            profile.credits.add(credit)
            senderProfileRepository.save(profile)
          }
        }
      }

      // Attach to prisoner profile
      val prisonerNumber = credit.prisonerNumber
      if (prisonerNumber != null) {
        val existing = prisonerProfileRepository.findByPrisonerNumber(prisonerNumber)
        val profile = if (existing.isNotEmpty()) existing.first() else PrisonerProfile(prisonerNumber = prisonerNumber)
        profile.prisonerName = credit.prisonerName
        profile.credits.add(credit)
        prisonerProfileRepository.save(profile)
      }
    }
  }
}
