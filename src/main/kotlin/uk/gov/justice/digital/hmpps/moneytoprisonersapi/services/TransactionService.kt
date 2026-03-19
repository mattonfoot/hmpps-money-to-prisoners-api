package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateTransactionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.TransactionRepository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * TXN-020 to TXN-030: Transaction business logic.
 */
@Service
class TransactionService(
  private val transactionRepository: TransactionRepository,
  private val creditRepository: CreditRepository,
  private val prisonRepository: PrisonRepository,
  private val privateEstateBatchRepository: PrivateEstateBatchRepository,
) {

  /**
   * TXN-020: Bulk creates transactions.
   * TXN-021: Auto-creates Credit for bank_transfer credit-category transactions.
   */
  @Transactional
  fun createTransactions(requests: List<CreateTransactionRequest>): List<Transaction> = requests.map { req ->
    val credit = if (req.category == TransactionCategory.CREDIT && req.source == TransactionSource.BANK_TRANSFER) {
      val newCredit = Credit(
        amount = req.amount,
        resolution = CreditResolution.PENDING,
        receivedAt = req.receivedAt,
        incompleteSenderInfo = req.incompleteSenderInfo,
      )
      newCredit.source = CreditSource.BANK_TRANSFER
      creditRepository.save(newCredit)
    } else {
      null
    }

    val transaction = Transaction(
      amount = req.amount,
      category = req.category,
      source = req.source,
      senderSortCode = req.senderSortCode,
      senderAccountNumber = req.senderAccountNumber,
      senderName = req.senderName,
      senderRollNumber = req.senderRollNumber,
      reference = req.reference,
      receivedAt = req.receivedAt,
      refCode = req.refCode,
      incompleteSenderInfo = req.incompleteSenderInfo,
      referenceInSenderField = req.referenceInSenderField,
      processorTypeCode = req.processorTypeCode,
      credit = credit,
    )
    transactionRepository.save(transaction)
  }

  /**
   * TXN-025 to TXN-027: List transactions with optional filters.
   */
  @Transactional(readOnly = true)
  fun listTransactions(
    status: TransactionStatus? = null,
    receivedAtGte: LocalDateTime? = null,
    receivedAtLt: LocalDateTime? = null,
    ids: List<Long>? = null,
  ): List<Transaction> {
    val all = when {
      ids != null && ids.isNotEmpty() -> transactionRepository.findByIdIn(ids)
      receivedAtGte != null && receivedAtLt != null ->
        transactionRepository.findByReceivedAtGreaterThanEqualAndReceivedAtLessThan(receivedAtGte, receivedAtLt)
      receivedAtGte != null -> transactionRepository.findByReceivedAtGreaterThanEqual(receivedAtGte)
      receivedAtLt != null -> transactionRepository.findByReceivedAtLessThan(receivedAtLt)
      else -> transactionRepository.findAll()
    }

    return if (status != null) {
      all.filter { TransactionStatus.computeFrom(it) == status }
    } else {
      all
    }
  }

  /**
   * TXN-023 to TXN-024: Bulk refund transactions.
   * Returns list of transaction IDs with conflict (invalid credit state).
   */
  @Transactional
  fun refundTransactions(transactionIds: List<Long>): List<Long> {
    val transactions = transactionRepository.findByIdIn(transactionIds)
    val conflictIds = mutableListOf<Long>()

    for (txn in transactions) {
      val credit = txn.credit
      if (credit == null || TransactionStatus.computeFrom(txn) != TransactionStatus.REFUNDABLE) {
        conflictIds.add(txn.id!!)
        continue
      }
      try {
        credit.transitionResolution(CreditResolution.REFUNDED)
        creditRepository.save(credit)
      } catch (e: Exception) {
        conflictIds.add(txn.id!!)
      }
    }
    return conflictIds
  }

  /**
   * TXN-028 to TXN-030: Reconcile transactions in a date range.
   * Returns a map with batch details if transactions found, null if none found.
   */
  @Transactional
  fun reconcileTransactions(receivedAtGte: LocalDateTime, receivedAtLt: LocalDateTime): Map<String, Any>? {
    val transactions = transactionRepository.findByReceivedAtGreaterThanEqualAndReceivedAtLessThan(receivedAtGte, receivedAtLt)
    if (transactions.isEmpty()) return null

    val today = LocalDate.now()
    val batchesByPrison = mutableMapOf<String, PrivateEstateBatch>()

    for (txn in transactions) {
      val credit = txn.credit ?: continue
      val prisonId = credit.prison ?: continue
      val prison = prisonRepository.findById(prisonId).orElse(null) ?: continue
      if (!prison.privateEstate) continue

      val ref = "$prisonId/$today"
      val batch = batchesByPrison.getOrElse(ref) {
        privateEstateBatchRepository.findById(ref).orElseGet {
          PrivateEstateBatch(ref = ref, prison = prisonId, date = today, totalAmount = 0)
        }
      }
      batch.credits.add(credit)
      batch.totalAmount = batch.credits.sumOf { it.amount }
      batchesByPrison[ref] = batch
    }

    batchesByPrison.values.forEach { privateEstateBatchRepository.save(it) }

    return mapOf(
      "transaction_count" to transactions.size,
      "private_estate_batches" to batchesByPrison.keys.toList(),
    )
  }
}
