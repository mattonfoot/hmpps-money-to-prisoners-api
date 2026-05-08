package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import java.time.LocalDate

/**
 * CRD-190 to CRD-195: Credit Reconciliation.
 *
 * Reconciles a list of credits by setting reconciled=true, creating RECONCILED log entries,
 * and for credits in private prisons, attaching them to a PrivateEstateBatch for today.
 */
@Service
class ReconcileService(
  private val creditRepository: CreditRepository,
  private val logRepository: LogRepository,
  private val privateEstateBatchRepository: PrivateEstateBatchRepository,
  private val userRepository: AuthUserRepository,
) {

  @Transactional
  fun reconcile(creditIds: List<Long>, userId: String) {
    if (creditIds.isEmpty()) return

    val credits = creditRepository.findByIdInWithLock(creditIds)
    val today = LocalDate.now()
    val reconciler = userRepository.findByUsername(userId)

    for (credit in credits) {
      credit.reconciled = true
      creditRepository.save(credit)
      val log = Log()
      log.action = LogAction.RECONCILED.value
      log.credit = credit
      log.user = reconciler
      logRepository.save(log)

      val prison = credit.prison ?: continue
      if (!prison.privateEstate) continue

      // Django models the credit↔batch link via credit_credit.private_estate_batch_id
      // (FK on the credit). Find or create the batch for (prison, today), then point
      // the credit's FK at it.
      val batch = privateEstateBatchRepository.findByPrisonAndDate(prison, today)
        ?: privateEstateBatchRepository.save(
          PrivateEstateBatch().apply {
            this.prison = prison
            this.date = today
          },
        )
      credit.privateEstateBatch = batch
      creditRepository.save(credit)
    }
  }
}
