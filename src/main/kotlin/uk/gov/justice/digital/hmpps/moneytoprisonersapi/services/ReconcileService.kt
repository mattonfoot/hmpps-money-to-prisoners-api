package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import java.time.LocalDate

/**
 * CRD-190 to CRD-195: Credit Reconciliation.
 *
 * Reconciles a list of credits by setting reconciled=true, creating RECONCILED log entries,
 * and for credits in private prisons, creating or updating a PrivateEstateBatch for today.
 */
@Service
class ReconcileService(
  private val creditRepository: CreditRepository,
  private val prisonRepository: PrisonRepository,
  private val logRepository: LogRepository,
  private val privateEstateBatchRepository: PrivateEstateBatchRepository,
) {

  @Transactional
  fun reconcile(creditIds: List<Long>, userId: String) {
    if (creditIds.isEmpty()) return

    val credits = creditRepository.findByIdInWithLock(creditIds)
    val today = LocalDate.now()

    for (credit in credits) {
      credit.reconciled = true
      creditRepository.save(credit)
      logRepository.save(Log(action = LogAction.RECONCILED, credit = credit, userId = userId))

      val prisonId = credit.prison ?: continue
      val prison = prisonRepository.findById(prisonId).orElse(null) ?: continue
      if (!prison.privateEstate) continue

      val ref = "$prisonId/$today"
      val batch = privateEstateBatchRepository.findById(ref).orElseGet {
        PrivateEstateBatch(ref = ref, prison = prisonId, date = today, totalAmount = 0)
      }
      batch.credits.add(credit)
      batch.totalAmount = batch.credits.sumOf { it.amount }
      privateEstateBatchRepository.save(batch)
    }
  }
}
