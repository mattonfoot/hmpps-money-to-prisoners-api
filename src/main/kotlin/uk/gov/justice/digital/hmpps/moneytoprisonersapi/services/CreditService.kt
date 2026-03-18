package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreditActionItem
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.InvalidCreditStateException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
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
  private val logRepository: LogRepository,
) {

  fun listCompletedCredits(): List<Credit> = creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED))

  fun listAllCredits(): List<Credit> = creditRepository.findAll()

  fun listCredits(
    search: String? = null,
    simpleSearch: String? = null,
    ordering: String? = null,
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

    if (!search.isNullOrBlank()) {
      credits = applySearch(credits, search)
    }

    if (!simpleSearch.isNullOrBlank()) {
      credits = applySimpleSearch(credits, simpleSearch)
    }

    if (!ordering.isNullOrBlank()) {
      credits = applyOrdering(credits, ordering)
    }

    return credits
  }

  private val amountSearchRegex = Regex("^£?(\\d+(?:\\.\\d\\d)?)$")

  private fun matchesSearchWord(credit: Credit, word: String): Boolean {
    if (credit.prisonerName?.contains(word, ignoreCase = true) == true) return true
    if (credit.prisonerNumber?.contains(word, ignoreCase = true) == true) return true
    if (credit.transaction?.senderName?.contains(word, ignoreCase = true) == true) return true
    if (credit.payment?.cardholderName?.contains(word, ignoreCase = true) == true) return true

    val amountMatch = amountSearchRegex.find(word)
    if (amountMatch != null) {
      val amountStr = amountMatch.groupValues[1]
      if (amountStr.contains(".")) {
        val parts = amountStr.split(".")
        val pence = parts[0].toLong() * 100 + parts[1].toLong()
        if (credit.amount == pence) return true
      } else {
        if (credit.amount.toString().startsWith(amountStr)) return true
      }
    }

    if (word.length == 8) {
      val uuidStr = credit.payment?.uuid?.toString()?.replace("-", "") ?: ""
      if (uuidStr.startsWith(word, ignoreCase = true)) return true
    }

    return false
  }

  private fun applySearch(credits: List<Credit>, search: String): List<Credit> {
    val words = search.trim().split("\\s+".toRegex())
    return credits.filter { credit ->
      words.all { word -> matchesSearchWord(credit, word) }
    }
  }

  private fun applySimpleSearch(credits: List<Credit>, simpleSearch: String): List<Credit> {
    val term = simpleSearch.trim()
    return credits.filter { credit ->
      credit.transaction?.senderName?.contains(term, ignoreCase = true) == true ||
        credit.payment?.cardholderName?.contains(term, ignoreCase = true) == true ||
        credit.payment?.email?.contains(term, ignoreCase = true) == true ||
        credit.prisonerNumber?.contains(term, ignoreCase = true) == true
    }
  }

  private val allowedOrderingFields = setOf("created", "received_at", "amount", "prisoner_number", "prisoner_name")

  private fun applyOrdering(credits: List<Credit>, ordering: String): List<Credit> {
    val descending = ordering.startsWith("-")
    val field = ordering.removePrefix("-")
    if (field !in allowedOrderingFields) return credits

    val comparator: Comparator<Credit> = when (field) {
      "created" -> nullsLastComparator(descending) { it.created }
      "received_at" -> nullsLastComparator(descending) { it.receivedAt }
      "amount" -> if (descending) compareByDescending { it.amount } else compareBy { it.amount }
      "prisoner_number" -> nullsLastComparator(descending) { it.prisonerNumber }
      "prisoner_name" -> nullsLastComparator(descending) { it.prisonerName }
      else -> return credits
    }

    return credits.sortedWith(comparator)
  }

  private fun <T : Comparable<T>> nullsLastComparator(
    descending: Boolean,
    selector: (Credit) -> T?,
  ): Comparator<Credit> = Comparator { a, b ->
    val va = selector(a)
    val vb = selector(b)
    when {
      va == null && vb == null -> 0
      va == null -> 1
      vb == null -> -1
      descending -> vb.compareTo(va)
      else -> va.compareTo(vb)
    }
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

  /**
   * CRD-130 to CRD-136: Review action.
   *
   * Sets reviewed=true on all specified credits regardless of their current state.
   * Creates a REVIEWED log entry for each credit. Uses pessimistic locking
   * (select_for_update) for transaction safety.
   *
   * @param creditIds list of credit IDs to mark as reviewed
   * @param userId the username of the user performing the action
   */
  @Transactional
  fun review(creditIds: List<Long>, userId: String) {
    if (creditIds.isEmpty()) return

    val credits = creditRepository.findByIdInWithLock(creditIds)
    for (credit in credits) {
      credit.reviewed = true
      creditRepository.save(credit)
      logRepository.save(Log(action = LogAction.REVIEWED, credit = credit, userId = userId))
    }
  }

  /**
   * CRD-110 to CRD-115: Credit prisoners action.
   *
   * For each item with credited=true, checks if the credit is in credit_pending state.
   * If so, transitions it to CREDITED, sets owner and optional nomis_transaction_id,
   * and creates a CREDITED log entry. Credits not in credit_pending state are returned
   * as conflict IDs. Uses pessimistic locking (select_for_update) for transaction safety.
   *
   * @param items list of credit action items (id, credited flag, optional nomis_transaction_id)
   * @param userId the username of the user performing the action
   * @return list of credit IDs that could not be processed due to invalid state
   */
  @Transactional
  fun creditPrisoners(items: List<CreditActionItem>, userId: String): List<Long> {
    val creditedItems = items.filter { it.credited == true }
    if (creditedItems.isEmpty()) return emptyList()

    val ids = creditedItems.map { it.id!! }
    val creditMap = creditRepository.findByIdInWithLock(ids).associateBy { it.id!! }

    val conflictIds = mutableListOf<Long>()

    for (item in creditedItems) {
      val credit = creditMap[item.id!!]
      if (credit == null || CreditStatus.computeFrom(credit) != CreditStatus.CREDIT_PENDING) {
        conflictIds.add(item.id)
        continue
      }

      credit.resolution = CreditResolution.CREDITED
      credit.owner = userId
      if (item.nomisTransactionId != null) {
        credit.nomisTransactionId = item.nomisTransactionId
      }
      creditRepository.save(credit)

      logRepository.save(Log(action = LogAction.CREDITED, credit = credit, userId = userId))
    }

    return conflictIds
  }

  /**
   * CRD-120 to CRD-125: Set manual action.
   *
   * For each credit ID, checks if the credit has resolution=pending.
   * If so, transitions it to MANUAL, sets owner to the requesting user,
   * and creates a MANUAL log entry. Credits not in pending state are returned
   * as conflict IDs. Uses pessimistic locking (select_for_update) for transaction safety.
   *
   * @param creditIds list of credit IDs to set to manual
   * @param userId the username of the user performing the action
   * @return list of credit IDs that could not be processed due to invalid state
   */
  @Transactional
  fun setManual(creditIds: List<Long>, userId: String): List<Long> {
    if (creditIds.isEmpty()) return emptyList()

    val creditMap = creditRepository.findByIdInWithLock(creditIds).associateBy { it.id!! }

    val conflictIds = mutableListOf<Long>()

    for (id in creditIds) {
      val credit = creditMap[id]
      if (credit == null || credit.resolution != CreditResolution.PENDING) {
        conflictIds.add(id)
        continue
      }

      credit.resolution = CreditResolution.MANUAL
      credit.owner = userId
      creditRepository.save(credit)

      logRepository.save(Log(action = LogAction.MANUAL, credit = credit, userId = userId))
    }

    return conflictIds
  }

  /**
   * CRD-140 to CRD-144: Refund action.
   *
   * For each credit ID, checks if the credit is in refund_pending status
   * ((no prison OR blocked) AND pending resolution AND sender info complete).
   * If eligible, transitions it to REFUNDED and creates a REFUNDED log entry.
   * If any credit is not in refund_pending status, throws InvalidCreditStateException
   * (strict validation — no partial processing). Uses pessimistic locking
   * (select_for_update) for transaction safety.
   *
   * @param creditIds list of credit IDs to refund
   * @param userId the username of the user performing the action
   * @throws InvalidCreditStateException if any credit is not in refund_pending state
   */
  @Transactional
  fun refund(creditIds: List<Long>, userId: String) {
    if (creditIds.isEmpty()) return

    val creditMap = creditRepository.findByIdInWithLock(creditIds).associateBy { it.id!! }

    for (id in creditIds) {
      val credit = creditMap[id]
      if (credit == null || CreditStatus.computeFrom(credit) != CreditStatus.REFUND_PENDING) {
        throw InvalidCreditStateException(
          credit?.resolution ?: CreditResolution.INITIAL,
          CreditResolution.REFUNDED,
        )
      }

      credit.resolution = CreditResolution.REFUNDED
      creditRepository.save(credit)

      logRepository.save(Log(action = LogAction.REFUNDED, credit = credit, userId = userId))
    }
  }
}
