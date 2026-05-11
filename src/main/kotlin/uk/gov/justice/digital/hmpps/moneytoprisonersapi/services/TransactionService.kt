package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateTransactionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.transitionResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.TransactionRepository
import java.time.LocalDate
import java.time.OffsetDateTime

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
      val newCredit = Credit().apply {
        amount = req.amount
        resolution = CreditResolution.PENDING.value
        receivedAt = req.receivedAt
        // Django's credit_credit has no incompleteSenderInfo column — it's on
        // transaction_transaction. The flag here is captured below on the txn.
      }
      creditRepository.save(newCredit)
    } else {
      null
    }

    val transaction = Transaction().apply {
      amount = req.amount
      category = req.category.value
      source = req.source.value
      senderSortCode = req.senderSortCode.orEmpty()
      senderAccountNumber = req.senderAccountNumber.orEmpty()
      senderName = req.senderName.orEmpty()
      senderRollNumber = req.senderRollNumber.orEmpty()
      reference = req.reference.orEmpty()
      receivedAt = req.receivedAt ?: OffsetDateTime.now()
      refCode = req.refCode
      incompleteSenderInfo = req.incompleteSenderInfo
      referenceInSenderField = req.referenceInSenderField
      processorTypeCode = req.processorTypeCode
      this.credit = credit
    }
    transactionRepository.save(transaction)
  }

  /**
   * TXN-025 to TXN-027: List transactions with optional filters.
   */
  @Transactional(readOnly = true)
  fun listTransactions(
    status: TransactionStatus? = null,
    receivedAtGte: OffsetDateTime? = null,
    receivedAtLt: OffsetDateTime? = null,
    ids: List<Long>? = null,
  ): List<Transaction> {
    val gte = receivedAtGte
    val lt = receivedAtLt
    val all = when {
      ids != null && ids.isNotEmpty() -> transactionRepository.findByIdIn(ids)
      gte != null && lt != null ->
        transactionRepository.findByReceivedAtGreaterThanEqualAndReceivedAtLessThan(gte, lt)
      gte != null -> transactionRepository.findByReceivedAtGreaterThanEqual(gte)
      lt != null -> transactionRepository.findByReceivedAtLessThan(lt)
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
  fun reconcileTransactions(receivedAtGte: OffsetDateTime, receivedAtLt: OffsetDateTime): Map<String, Any>? {
    // Django's credit_privateestatebatch is keyed by an auto id and uses a
    // (date, prison_id) unique constraint. The Kotlin `ref = "$prisonId/$date"`
    // composite-key approach used previously doesn't apply. Reconcile against
    // Django's shape: look up by (prison, date), create if absent, append credits.
    val transactions = transactionRepository.findByReceivedAtGreaterThanEqualAndReceivedAtLessThan(receivedAtGte, receivedAtLt)
    if (transactions.isEmpty()) return null

    val today = LocalDate.now()
    val batchesByPrison = mutableMapOf<String, PrivateEstateBatch>()

    for (txn in transactions) {
      val credit = txn.credit ?: continue
      val prison = credit.prison ?: continue
      if (!prison.privateEstate) continue
      val prisonId = prison.nomisId

      val batch = batchesByPrison.getOrPut(prisonId) {
        privateEstateBatchRepository.findByPrisonAndDate(prison, today)
          ?: PrivateEstateBatch().apply {
            this.prison = prison
            this.date = today
          }
      }
      // The credit↔batch link is owned by Credit's private_estate_batch_id
      // FK (the @OneToMany on the batch is the inverse side). Set it on the
      // credit so the FK actually persists.
      credit.privateEstateBatch = batch
      batch.credits.add(credit)
      batchesByPrison[prisonId] = batch
    }

    batchesByPrison.values.forEach { privateEstateBatchRepository.save(it) }
    // Re-save credits so the new private_estate_batch_id FK is persisted.
    for (txn in transactions) {
      txn.credit?.let { creditRepository.save(it) }
    }

    return mapOf(
      "transaction_count" to transactions.size,
      "private_estate_batches" to batchesByPrison.keys.toList(),
    )
  }
}
