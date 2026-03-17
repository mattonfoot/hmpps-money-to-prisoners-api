package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CreditNotFoundException(id: Long) : RuntimeException("Credit not found with id: $id")

@Service
class CreditService(
  private val creditRepository: CreditRepository,
  private val prisonRepository: PrisonRepository,
  private val senderProfileRepository: SenderProfileRepository,
  private val prisonerProfileRepository: PrisonerProfileRepository,
) {

  fun listCompletedCredits(): List<Credit> = creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED))

  fun listAllCredits(): List<Credit> = creditRepository.findAll()

  fun listCredits(
    status: CreditStatus? = null,
    prisons: List<String>? = null,
    prisonIsNull: Boolean? = null,
    prisonRegion: String? = null,
    prisonCategory: String? = null,
    prisonPopulation: String? = null,
    amount: Long? = null,
    amountGte: Long? = null,
    amountLte: Long? = null,
    amountEndswith: String? = null,
    amountRegex: String? = null,
    excludeAmountEndswith: String? = null,
    excludeAmountRegex: String? = null,
    prisonerName: String? = null,
    prisonerNumber: String? = null,
    user: String? = null,
    resolution: CreditResolution? = null,
    reviewed: Boolean? = null,
    receivedAtGte: LocalDateTime? = null,
    receivedAtLt: LocalDateTime? = null,
    valid: Boolean? = null,
    senderName: String? = null,
    senderSortCode: String? = null,
    senderAccountNumber: String? = null,
    senderRollNumber: String? = null,
    senderNameIsBlank: Boolean? = null,
    senderSortCodeIsBlank: Boolean? = null,
    senderEmail: String? = null,
    senderIpAddress: String? = null,
    cardNumberFirstDigits: String? = null,
    cardNumberLastDigits: String? = null,
    cardExpiryDate: String? = null,
    senderPostcode: String? = null,
    paymentReference: String? = null,
    source: CreditSource? = null,
    loggedAtGte: LocalDateTime? = null,
    loggedAtLt: LocalDateTime? = null,
    securityCheckIsnull: Boolean? = null,
    securityCheckActionedByIsnull: Boolean? = null,
    excludeCreditIn: List<Long>? = null,
    monitored: Boolean? = null,
    pk: List<Long>? = null,
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

    if (!prisons.isNullOrEmpty()) {
      val prisonSet = prisons.toSet()
      credits = credits.filter { it.prison in prisonSet }
    }

    if (prisonIsNull == true) {
      credits = credits.filter { it.prison == null }
    }

    if (prisonRegion != null) {
      val matchingPrisonIds = prisonRepository.findByRegionContainingIgnoreCase(prisonRegion)
        .map { it.nomisId }
        .toSet()
      credits = credits.filter { it.prison in matchingPrisonIds }
    }

    if (prisonCategory != null) {
      val matchingPrisonIds = prisonRepository.findByCategoryName(prisonCategory)
        .map { it.nomisId }
        .toSet()
      credits = credits.filter { it.prison in matchingPrisonIds }
    }

    if (prisonPopulation != null) {
      val matchingPrisonIds = prisonRepository.findByPopulationName(prisonPopulation)
        .map { it.nomisId }
        .toSet()
      credits = credits.filter { it.prison in matchingPrisonIds }
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

    if (amountEndswith != null) {
      credits = credits.filter { it.amount.toString().endsWith(amountEndswith) }
    }

    if (amountRegex != null) {
      val regex = Regex(amountRegex)
      credits = credits.filter { regex.containsMatchIn(it.amount.toString()) }
    }

    if (excludeAmountEndswith != null) {
      credits = credits.filter { !it.amount.toString().endsWith(excludeAmountEndswith) }
    }

    if (excludeAmountRegex != null) {
      val regex = Regex(excludeAmountRegex)
      credits = credits.filter { !regex.containsMatchIn(it.amount.toString()) }
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

    if (senderName != null) {
      credits = credits.filter {
        it.transaction?.senderName?.contains(senderName, ignoreCase = true) == true ||
          it.payment?.cardholderName?.contains(senderName, ignoreCase = true) == true
      }
    }

    if (senderSortCode != null) {
      credits = credits.filter { it.transaction?.senderSortCode == senderSortCode }
    }

    if (senderAccountNumber != null) {
      credits = credits.filter { it.transaction?.senderAccountNumber == senderAccountNumber }
    }

    if (senderRollNumber != null) {
      credits = credits.filter { it.transaction?.senderRollNumber == senderRollNumber }
    }

    if (senderNameIsBlank == true) {
      credits = credits.filter {
        it.transaction != null && it.transaction!!.senderName.isNullOrEmpty()
      }
    }

    if (senderSortCodeIsBlank == true) {
      credits = credits.filter {
        it.transaction != null && it.transaction!!.senderSortCode.isNullOrEmpty()
      }
    }

    if (senderEmail != null) {
      credits = credits.filter {
        it.payment?.email?.contains(senderEmail, ignoreCase = true) == true
      }
    }

    if (senderIpAddress != null) {
      credits = credits.filter { it.payment?.ipAddress == senderIpAddress }
    }

    if (cardNumberFirstDigits != null) {
      credits = credits.filter { it.payment?.cardNumberFirstDigits == cardNumberFirstDigits }
    }

    if (cardNumberLastDigits != null) {
      credits = credits.filter { it.payment?.cardNumberLastDigits == cardNumberLastDigits }
    }

    if (cardExpiryDate != null) {
      credits = credits.filter { it.payment?.cardExpiryDate == cardExpiryDate }
    }

    if (senderPostcode != null) {
      val normalizedFilter = senderPostcode.replace("\\s".toRegex(), "").lowercase()
      credits = credits.filter {
        val postcode = it.payment?.billingAddress?.postcode
        postcode != null && postcode.replace("\\s".toRegex(), "").lowercase() == normalizedFilter
      }
    }

    if (paymentReference != null) {
      credits = credits.filter {
        it.payment?.uuid?.toString()?.startsWith(paymentReference) == true
      }
    }

    if (source != null) {
      credits = when (source) {
        CreditSource.BANK_TRANSFER -> credits.filter { it.transaction != null }
        CreditSource.ONLINE -> credits.filter { it.payment != null }
        CreditSource.UNKNOWN -> credits.filter { it.transaction == null && it.payment == null }
      }
    }

    if (loggedAtGte != null) {
      val gteDate = loggedAtGte.toLocalDate()
      credits = credits.filter { credit ->
        credit.logs.any { log -> log.created != null && !log.created!!.toLocalDate().isBefore(gteDate) }
      }
    }

    if (loggedAtLt != null) {
      val ltDate = loggedAtLt.toLocalDate()
      credits = credits.filter { credit ->
        credit.logs.any { log -> log.created != null && log.created!!.toLocalDate().isBefore(ltDate) }
      }
    }

    if (securityCheckIsnull != null) {
      credits = if (securityCheckIsnull) {
        credits.filter { it.securityCheck == null }
      } else {
        credits.filter { it.securityCheck != null }
      }
    }

    if (securityCheckActionedByIsnull != null) {
      credits = if (securityCheckActionedByIsnull) {
        credits.filter { it.securityCheck != null && it.securityCheck!!.actionedBy == null }
      } else {
        credits.filter { it.securityCheck != null && it.securityCheck!!.actionedBy != null }
      }
    }

    if (!excludeCreditIn.isNullOrEmpty()) {
      val excludeSet = excludeCreditIn.toSet()
      credits = credits.filter { it.id !in excludeSet }
    }

    if (monitored == true) {
      val monitoredCreditIds = senderProfileRepository.findCreditIdsWithMonitoredSenderProfiles() +
        prisonerProfileRepository.findCreditIdsWithMonitoredPrisonerProfiles()
      credits = credits.filter { it.id in monitoredCreditIds }
    }

    if (!pk.isNullOrEmpty()) {
      val pkSet = pk.toSet()
      credits = credits.filter { it.id in pkSet }
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
