package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CreditNotFoundException(id: Long) : RuntimeException("Credit not found with id: $id")

@Service
class CreditService(
  private val creditRepository: CreditRepository,
) {

  fun listCompletedCredits(): List<Credit> = creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED))

  fun listAllCredits(): List<Credit> = creditRepository.findAll()

  fun createCredit(
    amount: Long,
    prisonerNumber: String?,
    prisonerName: String?,
    prisonerDob: LocalDate?,
    receivedAt: LocalDateTime?,
    source: CreditSource,
  ): Credit {
    val credit = Credit(
      amount = amount,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      prisonerDob = prisonerDob,
      receivedAt = receivedAt,
      resolution = CreditResolution.INITIAL,
    )
    credit.source = source
    return creditRepository.save(credit)
  }

  fun transitionResolution(creditId: Long, newResolution: CreditResolution): Credit {
    val credit = creditRepository.findById(creditId)
      .orElseThrow { CreditNotFoundException(creditId) }
    credit.transitionResolution(newResolution)
    return creditRepository.save(credit)
  }

  fun computeStatus(credit: Credit): CreditStatus = CreditStatus.computeFrom(credit)
}
