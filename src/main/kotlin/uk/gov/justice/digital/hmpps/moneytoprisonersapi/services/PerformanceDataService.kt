package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PerformanceDataDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PerformanceDataResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PerformanceData
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PerformanceDataRepository
import java.time.LocalDate
import kotlin.math.roundToInt

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

    val results = performanceDataRepository.findByWeekBetween(effectiveGte, effectiveLt)
      .map { it.toDto() }

    return PerformanceDataResponse(
      headers = PerformanceData.HEADERS,
      results = results,
    )
  }

  private fun PerformanceData.toDto() = PerformanceDataDto(
    week = week,
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
