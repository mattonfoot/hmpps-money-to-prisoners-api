package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.DisbursementNotFoundException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.DisbursementNotPendingException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementActionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementConfirmRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementLog
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.transitionResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementLogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import java.time.LocalDateTime

private const val INVOICE_NUMBER_BASE = 1000000L

@Service
class DisbursementService(
  private val disbursementRepository: DisbursementRepository,
  private val disbursementLogRepository: DisbursementLogRepository,
  private val prisonerProfileRepository: PrisonerProfileRepository,
  private val prisonRepository: PrisonRepository,
  private val userRepository: AuthUserRepository,
) {

  fun getDisbursement(id: Long): Disbursement? = disbursementRepository.findById(id).orElse(null)

  fun listDisbursements(
    amount: Long? = null,
    amountGte: Long? = null,
    amountLte: Long? = null,
    amountEndswith: String? = null,
    amountRegex: String? = null,
    excludeAmountEndswith: String? = null,
    excludeAmountRegex: String? = null,
    resolution: List<DisbursementResolution>? = null,
    method: DisbursementMethod? = null,
    prisonerNumber: String? = null,
    prisonerName: String? = null,
    recipientName: String? = null,
    recipientEmail: String? = null,
    recipientIsCompany: Boolean? = null,
    prisons: List<String>? = null,
    prisonRegion: String? = null,
    prisonCategory: String? = null,
    prisonPopulation: String? = null,
    sortCode: String? = null,
    accountNumber: String? = null,
    rollNumber: String? = null,
    postcode: String? = null,
    city: String? = null,
    invoiceNumber: String? = null,
    nomisTransactionId: String? = null,
    created: LocalDateTime? = null,
    createdGte: LocalDateTime? = null,
    createdLt: LocalDateTime? = null,
    loggedAtGte: LocalDateTime? = null,
    loggedAtLt: LocalDateTime? = null,
    logAction: String? = null,
    simpleSearch: String? = null,
    ordering: String? = null,
    monitoredByUsername: String? = null,
  ): List<Disbursement> {
    var disbursements = disbursementRepository.findAll()

    if (amount != null) {
      disbursements = disbursements.filter { it.amount.toLong() == amount }
    }

    if (amountGte != null) {
      disbursements = disbursements.filter { it.amount.toLong() >= amountGte }
    }

    if (amountLte != null) {
      disbursements = disbursements.filter { it.amount.toLong() <= amountLte }
    }

    if (!resolution.isNullOrEmpty()) {
      val resolutionValues = resolution.map { it.value }.toSet()
      disbursements = disbursements.filter { it.resolution in resolutionValues }
    }

    if (method != null) {
      disbursements = disbursements.filter { it.method == method.value }
    }

    if (prisonerNumber != null) {
      disbursements = disbursements.filter { it.prisonerNumber?.equals(prisonerNumber, ignoreCase = true) == true }
    }

    if (prisonerName != null) {
      disbursements = disbursements.filter { it.prisonerName?.contains(prisonerName, ignoreCase = true) == true }
    }

    if (recipientName != null) {
      disbursements = disbursements.filter {
        "${it.recipientFirstName} ${it.recipientLastName}".contains(recipientName, ignoreCase = true)
      }
    }

    if (!prisons.isNullOrEmpty()) {
      val prisonSet = prisons.toSet()
      disbursements = disbursements.filter { it.prison?.nomisId in prisonSet }
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

    if (city != null) {
      disbursements = disbursements.filter { it.city?.contains(city, ignoreCase = true) == true }
    }

    if (recipientEmail != null) {
      disbursements = disbursements.filter { it.recipientEmail?.contains(recipientEmail, ignoreCase = true) == true }
    }

    if (recipientIsCompany != null) {
      disbursements = disbursements.filter { it.recipientIsCompany == recipientIsCompany }
    }

    if (invoiceNumber != null) {
      disbursements = disbursements.filter { it.invoiceNumber == invoiceNumber }
    }

    if (nomisTransactionId != null) {
      disbursements = disbursements.filter { it.nomisTransactionId == nomisTransactionId }
    }

    if (created != null) {
      val date = created.toLocalDate()
      disbursements = disbursements.filter { it.created?.toLocalDate() == date }
    }

    if (createdGte != null) {
      val createdGteOffset = createdGte.atOffset(java.time.ZoneOffset.UTC)
      disbursements = disbursements.filter { !it.created.isBefore(createdGteOffset) }
    }

    if (createdLt != null) {
      val createdLtOffset = createdLt.atOffset(java.time.ZoneOffset.UTC)
      disbursements = disbursements.filter { it.created.isBefore(createdLtOffset) }
    }

    if (loggedAtGte != null) {
      val gteDate = loggedAtGte.toLocalDate()
      disbursements = disbursements.filter { d ->
        d.logs.any { log -> !log.created.toLocalDate().isBefore(gteDate) }
      }
    }

    if (loggedAtLt != null) {
      val ltDate = loggedAtLt.toLocalDate()
      disbursements = disbursements.filter { d ->
        d.logs.any { log -> log.created.toLocalDate().isBefore(ltDate) }
      }
    }

    if (logAction != null) {
      disbursements = disbursements.filter { d ->
        d.logs.any { log -> log.action == logAction }
      }
    }

    if (prisonRegion != null) {
      val matchingPrisonIds = prisonRepository.findByRegionContainingIgnoreCase(prisonRegion)
        .map { it.nomisId }.toSet()
      disbursements = disbursements.filter { it.prison?.nomisId in matchingPrisonIds }
    }

    if (prisonCategory != null) {
      val matchingPrisonIds = prisonRepository.findByCategoryName(prisonCategory)
        .map { it.nomisId }.toSet()
      disbursements = disbursements.filter { it.prison?.nomisId in matchingPrisonIds }
    }

    if (prisonPopulation != null) {
      val matchingPrisonIds = prisonRepository.findByPopulationName(prisonPopulation)
        .map { it.nomisId }.toSet()
      disbursements = disbursements.filter { it.prison?.nomisId in matchingPrisonIds }
    }

    if (amountEndswith != null) {
      disbursements = disbursements.filter { it.amount.toString().endsWith(amountEndswith) }
    }

    if (amountRegex != null) {
      val regex = Regex(amountRegex)
      disbursements = disbursements.filter { regex.containsMatchIn(it.amount.toString()) }
    }

    if (excludeAmountEndswith != null) {
      disbursements = disbursements.filter { !it.amount.toString().endsWith(excludeAmountEndswith) }
    }

    if (excludeAmountRegex != null) {
      val regex = Regex(excludeAmountRegex)
      disbursements = disbursements.filter { !regex.containsMatchIn(it.amount.toString()) }
    }

    if (!simpleSearch.isNullOrBlank()) {
      val term = simpleSearch.trim()
      disbursements = disbursements.filter { d ->
        d.prisonerName.contains(term, ignoreCase = true) ||
          d.prisonerNumber.contains(term, ignoreCase = true) ||
          "${d.recipientFirstName} ${d.recipientLastName}".contains(term, ignoreCase = true)
      }
    }

    if (monitoredByUsername != null) {
      // Django stores monitoring user IDs as integers (FK to auth_user). Resolve
      // the username to its user id, then filter prisoner profiles by that id.
      val monitoringUserId = userRepository.findByUsername(monitoredByUsername)?.id
      val monitoredPrisonerNumbers = if (monitoringUserId == null) {
        emptySet()
      } else {
        prisonerProfileRepository.findAll()
          .filter { it.monitoringUsers.contains(monitoringUserId.toInt()) }
          .mapNotNull { it.prisonerNumber }
          .toSet()
      }
      disbursements = disbursements.filter { it.prisonerNumber in monitoredPrisonerNumbers }
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
      "resolution" -> if (descending) compareByDescending { it.resolution } else compareBy { it.resolution }
      "method" -> if (descending) compareByDescending { it.method } else compareBy { it.method }
      "prisoner_name" -> nullsLastComparator(descending) { it.prisonerName }
      "recipient_name" -> nullsLastComparator(descending) { "${it.recipientFirstName} ${it.recipientLastName}" }
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
    // Mirrors mtp_api/apps/disbursement/serializers.py DisbursementSerializer.create:
    // resolves prison FK, applies prisoner_name lookup elsewhere (via PrisonerLocation
    // when called from controller), sets resolution to PENDING, then logs CREATED.
    val prisonEntity = request.prison?.let { prisonRepository.findById(it).orElse(null) }
    val disbursement = Disbursement().apply {
      amount = request.amount.toInt()
      method = request.method.value
      prison = prisonEntity
      prisonerNumber = request.prisonerNumber.orEmpty()
      prisonerName = request.prisonerName.orEmpty()
      recipientFirstName = request.recipientFirstName.orEmpty()
      recipientLastName = request.recipientLastName.orEmpty()
      recipientEmail = request.recipientEmail
      addressLine1 = request.addressLine1
      addressLine2 = request.addressLine2
      city = request.city
      postcode = request.postcode
      country = request.country
      sortCode = request.sortCode
      accountNumber = request.accountNumber
      rollNumber = request.rollNumber
      recipientIsCompany = request.recipientIsCompany
      resolution = DisbursementResolution.PENDING.value
    }

    val saved = disbursementRepository.save(disbursement)
    val log = DisbursementLog()
    log.action = LogAction.CREATED.value
    log.disbursement = saved
    log.user = userRepository.findByUsername(userId)
    disbursementLogRepository.save(log)
    return saved
  }

  @Transactional
  fun updateDisbursement(id: Long, request: UpdateDisbursementRequest, userId: String): Disbursement {
    // Mirrors mtp_api/apps/disbursement/views.py DisbursementView.update — only
    // pending disbursements are editable; emit EDITED log if any field changed.
    val disbursement = disbursementRepository.findById(id)
      .orElseThrow { DisbursementNotFoundException(id) }

    val currentResolution = DisbursementResolution.fromValue(disbursement.resolution)
    if (currentResolution != DisbursementResolution.PENDING) {
      throw DisbursementNotPendingException(id, currentResolution)
    }

    var changed = false
    fun <T : Any> setIfChanged(newVal: T?, getter: () -> T?, setter: (T) -> Unit) {
      if (newVal != null && newVal != getter()) {
        setter(newVal)
        changed = true
      }
    }

    setIfChanged(request.amount?.toInt(), { disbursement.amount }) { disbursement.amount = it }
    setIfChanged(request.method?.value, { disbursement.method }) { disbursement.method = it }
    request.prison?.let { newNomisId ->
      if (newNomisId != disbursement.prison?.nomisId) {
        disbursement.prison = prisonRepository.findById(newNomisId).orElse(null)
        changed = true
      }
    }
    setIfChanged(request.prisonerNumber, { disbursement.prisonerNumber }) { disbursement.prisonerNumber = it }
    setIfChanged(request.prisonerName, { disbursement.prisonerName }) { disbursement.prisonerName = it }
    setIfChanged(request.recipientFirstName, { disbursement.recipientFirstName }) { disbursement.recipientFirstName = it }
    setIfChanged(request.recipientLastName, { disbursement.recipientLastName }) { disbursement.recipientLastName = it }
    setIfChanged(request.recipientEmail, { disbursement.recipientEmail }) { disbursement.recipientEmail = it }
    setIfChanged(request.addressLine1, { disbursement.addressLine1 }) { disbursement.addressLine1 = it }
    setIfChanged(request.addressLine2, { disbursement.addressLine2 }) { disbursement.addressLine2 = it }
    setIfChanged(request.city, { disbursement.city }) { disbursement.city = it }
    setIfChanged(request.postcode, { disbursement.postcode }) { disbursement.postcode = it }
    setIfChanged(request.country, { disbursement.country }) { disbursement.country = it }
    setIfChanged(request.sortCode, { disbursement.sortCode }) { disbursement.sortCode = it }
    setIfChanged(request.accountNumber, { disbursement.accountNumber }) { disbursement.accountNumber = it }
    setIfChanged(request.rollNumber, { disbursement.rollNumber }) { disbursement.rollNumber = it }
    setIfChanged(request.recipientIsCompany, { disbursement.recipientIsCompany }) { disbursement.recipientIsCompany = it }

    val saved = disbursementRepository.save(disbursement)

    if (changed) {
      val log = DisbursementLog()
      log.action = LogAction.EDITED.value
      log.disbursement = saved
      log.user = userRepository.findByUsername(userId)
      disbursementLogRepository.save(log)
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
      DisbursementLog().also { it.action = LogAction.REJECTED.value; it.disbursement = disbursement; it.user = userRepository.findByUsername(userId) }.let(disbursementLogRepository::save)
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
      DisbursementLog().also { it.action = LogAction.PRECONFIRMED.value; it.disbursement = disbursement; it.user = userRepository.findByUsername(userId) }.let(disbursementLogRepository::save)
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
      DisbursementLog().also { it.action = LogAction.CREATED.value; it.disbursement = disbursement; it.user = userRepository.findByUsername(userId) }.let(disbursementLogRepository::save)
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
      DisbursementLog().also { it.action = LogAction.CONFIRMED.value; it.disbursement = disbursement; it.user = userRepository.findByUsername(userId) }.let(disbursementLogRepository::save)
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
      DisbursementLog().also { it.action = LogAction.SENT.value; it.disbursement = disbursement; it.user = userRepository.findByUsername(userId) }.let(disbursementLogRepository::save)
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
