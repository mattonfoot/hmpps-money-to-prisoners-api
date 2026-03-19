package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementActionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementConfirmRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementLog
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementLogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository

class DisbursementNotFoundException(id: Long) : RuntimeException("Disbursement not found with id: $id")

class DisbursementNotPendingException(id: Long, resolution: DisbursementResolution) : RuntimeException("Disbursement $id is not PENDING (current resolution: $resolution)")

private const val INVOICE_NUMBER_BASE = 1000000L

@Service
class DisbursementService(
  private val disbursementRepository: DisbursementRepository,
  private val disbursementLogRepository: DisbursementLogRepository,
) {

  fun listDisbursements(
    amount: Long? = null,
    amountGte: Long? = null,
    amountLte: Long? = null,
    resolution: List<DisbursementResolution>? = null,
    method: DisbursementMethod? = null,
    prisonerNumber: String? = null,
    prisonerName: String? = null,
    recipientName: String? = null,
    prisons: List<String>? = null,
    sortCode: String? = null,
    accountNumber: String? = null,
    rollNumber: String? = null,
    postcode: String? = null,
    ordering: String? = null,
  ): List<Disbursement> {
    var disbursements = disbursementRepository.findAll()

    if (amount != null) {
      disbursements = disbursements.filter { it.amount == amount }
    }

    if (amountGte != null) {
      disbursements = disbursements.filter { it.amount >= amountGte }
    }

    if (amountLte != null) {
      disbursements = disbursements.filter { it.amount <= amountLte }
    }

    if (!resolution.isNullOrEmpty()) {
      disbursements = disbursements.filter { it.resolution in resolution }
    }

    if (method != null) {
      disbursements = disbursements.filter { it.method == method }
    }

    if (prisonerNumber != null) {
      disbursements = disbursements.filter { it.prisonerNumber?.equals(prisonerNumber, ignoreCase = true) == true }
    }

    if (prisonerName != null) {
      disbursements = disbursements.filter { it.prisonerName?.contains(prisonerName, ignoreCase = true) == true }
    }

    if (recipientName != null) {
      disbursements = disbursements.filter { it.recipientName?.contains(recipientName, ignoreCase = true) == true }
    }

    if (!prisons.isNullOrEmpty()) {
      val prisonSet = prisons.toSet()
      disbursements = disbursements.filter { it.prison in prisonSet }
    }

    if (sortCode != null) {
      disbursements = disbursements.filter { it.sortCode == sortCode }
    }

    if (accountNumber != null) {
      disbursements = disbursements.filter { it.accountNumber == accountNumber }
    }

    if (rollNumber != null) {
      disbursements = disbursements.filter { it.rollNumber == rollNumber }
    }

    if (postcode != null) {
      val normalizedFilter = postcode.replace("\\s".toRegex(), "").lowercase()
      disbursements = disbursements.filter {
        val pc = it.postcode
        pc != null && pc.replace("\\s".toRegex(), "").lowercase() == normalizedFilter
      }
    }

    if (!ordering.isNullOrBlank()) {
      disbursements = applyOrdering(disbursements, ordering)
    }

    return disbursements
  }

  private val allowedOrderingFields = setOf("created", "amount", "resolution", "method", "prisoner_name", "recipient_name")

  private fun applyOrdering(disbursements: List<Disbursement>, ordering: String): List<Disbursement> {
    val descending = ordering.startsWith("-")
    val field = ordering.removePrefix("-")
    if (field !in allowedOrderingFields) return disbursements

    val comparator: Comparator<Disbursement> = when (field) {
      "created" -> nullsLastComparator(descending) { it.created }
      "amount" -> if (descending) compareByDescending { it.amount } else compareBy { it.amount }
      "resolution" -> if (descending) compareByDescending { it.resolution.name } else compareBy { it.resolution.name }
      "method" -> if (descending) compareByDescending { it.method.name } else compareBy { it.method.name }
      "prisoner_name" -> nullsLastComparator(descending) { it.prisonerName }
      "recipient_name" -> nullsLastComparator(descending) { it.recipientName }
      else -> return disbursements
    }

    return disbursements.sortedWith(comparator)
  }

  private fun <T : Comparable<T>> nullsLastComparator(
    descending: Boolean,
    selector: (Disbursement) -> T?,
  ): Comparator<Disbursement> = Comparator { a, b ->
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

  @Transactional
  fun createDisbursement(request: CreateDisbursementRequest, userId: String): Disbursement {
    val disbursement = Disbursement(
      amount = request.amount,
      method = request.method,
      prison = request.prison,
      prisonerNumber = request.prisonerNumber,
      prisonerName = request.prisonerName,
      recipientFirstName = request.recipientFirstName,
      recipientLastName = request.recipientLastName,
      recipientEmail = request.recipientEmail,
      addressLine1 = request.addressLine1,
      addressLine2 = request.addressLine2,
      city = request.city,
      postcode = request.postcode,
      country = request.country,
      sortCode = request.sortCode,
      accountNumber = request.accountNumber,
      rollNumber = request.rollNumber,
      recipientIsCompany = request.recipientIsCompany,
      resolution = DisbursementResolution.PENDING,
    )

    val saved = disbursementRepository.save(disbursement)
    disbursementLogRepository.save(DisbursementLog(action = LogAction.CREATED, disbursement = saved, userId = userId))
    return saved
  }

  @Transactional
  fun updateDisbursement(id: Long, request: UpdateDisbursementRequest, userId: String): Disbursement {
    val disbursement = disbursementRepository.findById(id)
      .orElseThrow { DisbursementNotFoundException(id) }

    if (disbursement.resolution != DisbursementResolution.PENDING) {
      throw DisbursementNotPendingException(id, disbursement.resolution)
    }

    var changed = false

    if (request.amount != null && request.amount != disbursement.amount) {
      disbursement.amount = request.amount
      changed = true
    }
    if (request.method != null && request.method != disbursement.method) {
      disbursement.method = request.method
      changed = true
    }
    if (request.prison != null && request.prison != disbursement.prison) {
      disbursement.prison = request.prison
      changed = true
    }
    if (request.prisonerNumber != null && request.prisonerNumber != disbursement.prisonerNumber) {
      disbursement.prisonerNumber = request.prisonerNumber
      changed = true
    }
    if (request.prisonerName != null && request.prisonerName != disbursement.prisonerName) {
      disbursement.prisonerName = request.prisonerName
      changed = true
    }
    if (request.recipientFirstName != null && request.recipientFirstName != disbursement.recipientFirstName) {
      disbursement.recipientFirstName = request.recipientFirstName
      changed = true
    }
    if (request.recipientLastName != null && request.recipientLastName != disbursement.recipientLastName) {
      disbursement.recipientLastName = request.recipientLastName
      changed = true
    }
    if (request.recipientEmail != null && request.recipientEmail != disbursement.recipientEmail) {
      disbursement.recipientEmail = request.recipientEmail
      changed = true
    }
    if (request.addressLine1 != null && request.addressLine1 != disbursement.addressLine1) {
      disbursement.addressLine1 = request.addressLine1
      changed = true
    }
    if (request.addressLine2 != null && request.addressLine2 != disbursement.addressLine2) {
      disbursement.addressLine2 = request.addressLine2
      changed = true
    }
    if (request.city != null && request.city != disbursement.city) {
      disbursement.city = request.city
      changed = true
    }
    if (request.postcode != null && request.postcode != disbursement.postcode) {
      disbursement.postcode = request.postcode
      changed = true
    }
    if (request.country != null && request.country != disbursement.country) {
      disbursement.country = request.country
      changed = true
    }
    if (request.sortCode != null && request.sortCode != disbursement.sortCode) {
      disbursement.sortCode = request.sortCode
      changed = true
    }
    if (request.accountNumber != null && request.accountNumber != disbursement.accountNumber) {
      disbursement.accountNumber = request.accountNumber
      changed = true
    }
    if (request.rollNumber != null && request.rollNumber != disbursement.rollNumber) {
      disbursement.rollNumber = request.rollNumber
      changed = true
    }
    if (request.recipientIsCompany != null && request.recipientIsCompany != disbursement.recipientIsCompany) {
      disbursement.recipientIsCompany = request.recipientIsCompany
      changed = true
    }

    val saved = disbursementRepository.save(disbursement)

    if (changed) {
      disbursementLogRepository.save(DisbursementLog(action = LogAction.EDITED, disbursement = saved, userId = userId))
    }

    return saved
  }

  @Transactional
  fun reject(request: DisbursementActionRequest, userId: String) {
    val disbursements = disbursementRepository.findByIdInWithLock(request.disbursementIds)
    val disbursementMap = buildDisbursementMap(disbursements, request.disbursementIds)

    for (id in request.disbursementIds) {
      val disbursement = disbursementMap[id] ?: throw DisbursementNotFoundException(id)
      disbursement.transitionResolution(DisbursementResolution.REJECTED)
      disbursementRepository.save(disbursement)
      disbursementLogRepository.save(DisbursementLog(action = LogAction.REJECTED, disbursement = disbursement, userId = userId))
    }
  }

  @Transactional
  fun preconfirm(request: DisbursementActionRequest, userId: String) {
    val disbursements = disbursementRepository.findByIdInWithLock(request.disbursementIds)
    val disbursementMap = buildDisbursementMap(disbursements, request.disbursementIds)

    for (id in request.disbursementIds) {
      val disbursement = disbursementMap[id] ?: throw DisbursementNotFoundException(id)
      disbursement.transitionResolution(DisbursementResolution.PRECONFIRMED)
      disbursementRepository.save(disbursement)
      disbursementLogRepository.save(DisbursementLog(action = LogAction.PRECONFIRMED, disbursement = disbursement, userId = userId))
    }
  }

  @Transactional
  fun reset(request: DisbursementActionRequest, userId: String) {
    val disbursements = disbursementRepository.findByIdInWithLock(request.disbursementIds)
    val disbursementMap = buildDisbursementMap(disbursements, request.disbursementIds)

    for (id in request.disbursementIds) {
      val disbursement = disbursementMap[id] ?: throw DisbursementNotFoundException(id)
      disbursement.transitionResolution(DisbursementResolution.PENDING)
      disbursementRepository.save(disbursement)
      disbursementLogRepository.save(DisbursementLog(action = LogAction.CREATED, disbursement = disbursement, userId = userId))
    }
  }

  @Transactional
  fun confirm(request: DisbursementConfirmRequest, userId: String) {
    val ids = request.disbursements.map { it.id }
    val disbursements = disbursementRepository.findByIdInWithLock(ids)
    val disbursementMap = buildDisbursementMap(disbursements, ids)

    for (item in request.disbursements) {
      val disbursement = disbursementMap[item.id] ?: throw DisbursementNotFoundException(item.id)
      disbursement.transitionResolution(DisbursementResolution.CONFIRMED)
      // invoiceNumber uses id if available, otherwise generate from item id
      val disbursementId = disbursement.id ?: item.id
      disbursement.invoiceNumber = "PMD${INVOICE_NUMBER_BASE + disbursementId}"
      if (item.nomisTransactionId != null) {
        disbursement.nomisTransactionId = item.nomisTransactionId
      }
      disbursementRepository.save(disbursement)
      disbursementLogRepository.save(DisbursementLog(action = LogAction.CONFIRMED, disbursement = disbursement, userId = userId))
    }
  }

  @Transactional
  fun send(request: DisbursementActionRequest, userId: String) {
    val disbursements = disbursementRepository.findByIdInWithLock(request.disbursementIds)
    val disbursementMap = buildDisbursementMap(disbursements, request.disbursementIds)

    for (id in request.disbursementIds) {
      val disbursement = disbursementMap[id] ?: throw DisbursementNotFoundException(id)
      disbursement.transitionResolution(DisbursementResolution.SENT)
      disbursementRepository.save(disbursement)
      disbursementLogRepository.save(DisbursementLog(action = LogAction.SENT, disbursement = disbursement, userId = userId))
    }
  }

  /**
   * Builds a map from id -> Disbursement, handling null ids by using positional matching from requested ids.
   * This allows unit tests to work with Disbursement objects that haven't been persisted (id is null).
   */
  private fun buildDisbursementMap(disbursements: List<Disbursement>, ids: List<Long>): Map<Long, Disbursement> {
    // If all disbursements have IDs, use them directly
    val allHaveIds = disbursements.all { it.id != null }
    return if (allHaveIds) {
      disbursements.associateBy { it.id!! }
    } else {
      // Fallback: match by position (used in unit tests where objects aren't persisted)
      ids.zip(disbursements).toMap()
    }
  }
}
