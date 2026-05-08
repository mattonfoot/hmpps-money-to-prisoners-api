package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PerformanceData
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PerformanceDataResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PerformanceDataRepository
import java.time.LocalDate
import kotlin.math.roundToInt
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PerformanceData as PerformanceDataEntity

@Service
class PerformanceDataService(
  private val performanceDataRepository: PerformanceDataRepository,
) {

  /**
   * PRF-020: Returns weekly performance data.
   * PRF-021: Defaults to last 52 weeks when weekGte/weekLt are not provided.
   * PRF-022: Filters by the supplied week range.
   * PRF-023: Converts [0,1] float percentage fields to formatted strings (e.g. "67%").
   * PRF-024: Response includes a headers map of field → verbose label.
   */
  fun getPerformanceData(weekGte: LocalDate?, weekLt: LocalDate?): PerformanceDataResponse {
    val today = LocalDate.now()
    val effectiveGte = weekGte ?: today.minusWeeks(52)
    val effectiveLt = weekLt ?: today

    val results = performanceDataRepository.findByIdBetween(effectiveGte, effectiveLt)
      .map { it.toDto() }

    return PerformanceDataResponse(
      headers = HEADERS,
      results = results,
    )
  }

  // Django's `performance_performancedatum` uses the `id` column (a date) as the
  // primary key; the legacy Kotlin DTO surfaces it as `week`.
  private fun PerformanceDataEntity.toDto() = PerformanceData(
    week = id,
    creditsTotal = creditsTotal,
    creditsByMtp = creditsByMtp,
    digitalTakeup = digitalTakeup?.formatPercentage(),
    completionRate = completionRate?.formatPercentage(),
    userSatisfaction = userSatisfaction?.formatPercentage(),
    rated1 = rated1,
    rated2 = rated2,
    rated3 = rated3,
    rated4 = rated4,
    rated5 = rated5,
  )
}

/** PRF-023: Converts a [0,1] float to a rounded percentage string, e.g. 0.6666 → "67%". */
internal fun Double.formatPercentage(): String = "${(this * 100).roundToInt()}%"

/** PRF-024: Verbose labels surfaced on /performance/data/ responses. */
internal val HEADERS: Map<String, String> = mapOf(
  "week" to "Week starting",
  "credits_total" to "Total credits to prisons",
  "credits_by_mtp" to "Credits via MTP",
  "digital_takeup" to "Digital take-up",
  "completion_rate" to "Completion rate",
  "user_satisfaction" to "User satisfaction",
  "rated_1" to "Rated 1",
  "rated_2" to "Rated 2",
  "rated_3" to "Rated 3",
  "rated_4" to "Rated 4",
  "rated_5" to "Rated 5",
)
