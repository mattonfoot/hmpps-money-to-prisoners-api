package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.CustomException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.TransactionRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.UserEventRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Mirrors the Django `delete_old_data` management command
 * (`mtp_api/apps/core/management/commands/delete_old_data.py`).
 *
 * Default cutoff is 7 years ago; an explicit cutoff must also be at least 7 years old.
 *
 * Scope of this first cut: bulk deletion of leaf records (Transactions, Payments, UserEvents).
 * Credit/Disbursement deletion with cascade through NotificationXEvent + SenderProfile/
 * PrisonerProfile/RecipientProfile/BankAccount/BillingAddress orphan cleanup remains deferred —
 * those need the join-table repositories and a careful schema-level cascade story.
 */
@Service
class DeleteOldDataService(
  private val transactionRepository: TransactionRepository,
  private val paymentRepository: PaymentRepository,
  private val userEventRepository: UserEventRepository,
) {

  data class DeleteOldDataSummary(
    val cutoff: OffsetDateTime,
    val transactionsDeleted: Int,
    val paymentsDeleted: Int,
    val userEventsDeleted: Int,
  )

  /**
   * DOD-001..003: Resolves the cutoff date. Defaults to 7×365 days before [today];
   * an explicit [before] must also be ≥ 7×365 days old or it is rejected.
   */
  fun resolveCutoffDate(before: LocalDate?, today: LocalDate = LocalDate.now()): LocalDate {
    val sevenYearsAgo = today.minusDays(SEVEN_YEARS_IN_DAYS)
    if (before == null) return sevenYearsAgo
    if (before.isAfter(sevenYearsAgo)) {
      throw CustomException(
        "\"--before\" must be older than 7 years ago (was $before, must be ≤ $sevenYearsAgo)",
        HttpStatus.BAD_REQUEST,
      )
    }
    return before
  }

  /** DOD-010: Deletes all transactions whose `received_at` is strictly before [cutoff]. */
  @Transactional
  fun deleteOldTransactions(cutoff: OffsetDateTime): Int {
    val olds = transactionRepository.findByReceivedAtLessThan(cutoff)
    if (olds.isEmpty()) return 0
    transactionRepository.deleteAll(olds)
    return olds.size
  }

  /** DOD-011: Deletes all payments whose `modified` is strictly before [cutoff]. */
  @Transactional
  fun deleteOldPayments(cutoff: OffsetDateTime): Int {
    val olds = paymentRepository.findByModifiedBefore(cutoff)
    if (olds.isEmpty()) return 0
    paymentRepository.deleteAll(olds)
    return olds.size
  }

  /** DOD-012: Deletes all user-events whose `timestamp` is strictly before [cutoff]. */
  @Transactional
  fun deleteOldUserEvents(cutoff: OffsetDateTime): Int {
    val olds = userEventRepository.findByTimestampBefore(cutoff)
    if (olds.isEmpty()) return 0
    userEventRepository.deleteAll(olds)
    return olds.size
  }

  /** DOD-020: Top-level command: deletes from every supported table and returns a summary. */
  @Transactional
  fun deleteOldData(before: LocalDate? = null, today: LocalDate = LocalDate.now()): DeleteOldDataSummary {
    val cutoffDate = resolveCutoffDate(before, today)
    val cutoff = cutoffDate.atStartOfDay().atOffset(ZoneOffset.UTC)
    return DeleteOldDataSummary(
      cutoff = cutoff,
      transactionsDeleted = deleteOldTransactions(cutoff),
      paymentsDeleted = deleteOldPayments(cutoff),
      userEventsDeleted = deleteOldUserEvents(cutoff),
    )
  }

  companion object {
    // Matches the Python command's `7 * 365` day calculation (not "7 calendar years")
    // so the boundary stays stable across DST/leap years.
    private const val SEVEN_YEARS_IN_DAYS: Long = 7L * 365L
  }
}
