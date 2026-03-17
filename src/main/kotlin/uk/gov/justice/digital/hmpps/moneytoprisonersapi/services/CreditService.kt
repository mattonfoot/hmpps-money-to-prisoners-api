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

  fun listCredits(
    status: CreditStatus? = null,
    prison: String? = null,
    prisonIsNull: Boolean? = null,
    amount: Long? = null,
    amountGte: Long? = null,
    amountLte: Long? = null,
    prisonerName: String? = null,
    prisonerNumber: String? = null,
    user: String? = null,
    resolution: CreditResolution? = null,
    reviewed: Boolean? = null,
    receivedAtGte: LocalDateTime? = null,
    receivedAtLt: LocalDateTime? = null,
    valid: Boolean? = null,
  ): List<Credit> {
    var credits = if (status != null || valid != null) {
      listAllCredits()
    } else {
      listCompletedCredits()
    }

    if (status != null) {
      credits = credits.filter { CreditStatus.computeFrom(it) == status }
    }

    if (valid != null) {
      val validStatuses = setOf(CreditStatus.CREDIT_PENDING, CreditStatus.CREDITED)
      credits = if (valid) {
        credits.filter { CreditStatus.computeFrom(it) in validStatuses }
      } else {
        credits.filter { CreditStatus.computeFrom(it) !in validStatuses }
      }
    }

    if (prison != null) {
      credits = credits.filter { it.prison == prison }
    }

    if (prisonIsNull == true) {
      credits = credits.filter { it.prison == null }
    }

    if (amount != null) {
      credits = credits.filter { it.amount == amount }
    }

    if (amountGte != null) {
      credits = credits.filter { it.amount >= amountGte }
    }

    if (amountLte != null) {
      credits = credits.filter { it.amount <= amountLte }
    }

    if (prisonerName != null) {
      credits = credits.filter { it.prisonerName?.contains(prisonerName, ignoreCase = true) == true }
    }

    if (prisonerNumber != null) {
      credits = credits.filter { it.prisonerNumber == prisonerNumber }
    }

    if (user != null) {
      credits = credits.filter { it.owner == user }
    }

    if (resolution != null) {
      credits = credits.filter { it.resolution == resolution }
    }

    if (reviewed != null) {
      credits = credits.filter { it.reviewed == reviewed }
    }

    if (receivedAtGte != null) {
      credits = credits.filter { it.receivedAt != null && !it.receivedAt!!.isBefore(receivedAtGte) }
    }

    if (receivedAtLt != null) {
      credits = credits.filter { it.receivedAt != null && it.receivedAt!!.isBefore(receivedAtLt) }
    }

    return credits
  }

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
