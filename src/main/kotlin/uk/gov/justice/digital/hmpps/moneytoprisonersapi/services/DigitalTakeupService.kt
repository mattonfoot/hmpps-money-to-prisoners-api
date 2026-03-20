package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DigitalTakeupRepository

/** Projection interface mapped from the native monthly aggregation query. */
interface MonthlyTakeupProjection {
  fun getMonth(): String
  fun getTotalCreditsByPost(): Long
  fun getTotalCreditsByMtp(): Long
}

/** Result of a per-month digital take-up aggregation (PRF-004). */
data class MonthlyTakeupResult(
  val month: String,
  val creditsByPost: Long,
  val creditsByMtp: Long,
  /** Null when the combined total is zero (PRF-003 semantics). */
  val digitalTakeup: Double?,
)

@Service
class DigitalTakeupService(
  private val digitalTakeupRepository: DigitalTakeupRepository,
) {

  /**
   * PRF-004: Returns per-month aggregated digital take-up across the prison set.
   * PRF-006: Pass excludePrivateEstate=true to omit private-estate prisons.
   */
  fun digitalTakeupPerMonth(excludePrivateEstate: Boolean = false): List<MonthlyTakeupResult> = digitalTakeupRepository.digitalTakeupPerMonth(excludePrivateEstate).map { projection ->
    val total = projection.getTotalCreditsByPost() + projection.getTotalCreditsByMtp()
    val takeup = if (total == 0L) null else projection.getTotalCreditsByMtp().toDouble() / total
    MonthlyTakeupResult(
      month = projection.getMonth(),
      creditsByPost = projection.getTotalCreditsByPost(),
      creditsByMtp = projection.getTotalCreditsByMtp(),
      digitalTakeup = takeup,
    )
  }

  /**
   * PRF-005: Returns the mean digital take-up across all records.
   * PRF-006: Pass excludePrivateEstate=true to omit private-estate prisons.
   */
  fun meanDigitalTakeup(excludePrivateEstate: Boolean = false): Double? = digitalTakeupRepository.meanDigitalTakeup(excludePrivateEstate)
}
