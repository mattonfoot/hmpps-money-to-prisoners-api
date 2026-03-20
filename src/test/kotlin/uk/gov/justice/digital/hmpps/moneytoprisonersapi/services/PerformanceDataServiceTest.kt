package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PerformanceData
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PerformanceDataRepository
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("PerformanceDataService")
class PerformanceDataServiceTest {

  @Mock
  private lateinit var performanceDataRepository: PerformanceDataRepository

  @InjectMocks
  private lateinit var service: PerformanceDataService

  private val monday = LocalDate.of(2024, 1, 1) // a Monday

  private fun makePerformanceData(
    week: LocalDate = monday,
    creditsTotal: Int? = 100,
    creditsByMtp: Int? = 80,
    digitalTakeup: Double? = 0.8,
    completionRate: Double? = 0.95,
    userSatisfaction: Double? = 0.666666,
  ) = PerformanceData(
    week = week,
    creditsTotal = creditsTotal,
    creditsByMtp = creditsByMtp,
    digitalTakeup = digitalTakeup,
    completionRate = completionRate,
    userSatisfaction = userSatisfaction,
  )

  // -------------------------------------------------------------------------
  // PRF-021: Default last 52 weeks when no date params provided
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("getPerformanceData defaults (PRF-021)")
  inner class Defaults {

    @Test
    fun `PRF-021 uses last 52 weeks by default when weekGte is null`() {
      whenever(performanceDataRepository.findByWeekBetween(any(), any())).thenReturn(emptyList())
      val today = LocalDate.now()
      val expectedGte = today.minusWeeks(52)

      service.getPerformanceData(weekGte = null, weekLt = null)

      val gteCaptor = argumentCaptor<LocalDate>()
      val ltCaptor = argumentCaptor<LocalDate>()
      verify(performanceDataRepository).findByWeekBetween(gteCaptor.capture(), ltCaptor.capture())
      assertThat(gteCaptor.firstValue).isEqualTo(expectedGte)
    }

    @Test
    fun `PRF-021 uses today as upper bound by default when weekLt is null`() {
      whenever(performanceDataRepository.findByWeekBetween(any(), any())).thenReturn(emptyList())
      val today = LocalDate.now()

      service.getPerformanceData(weekGte = null, weekLt = null)

      val ltCaptor = argumentCaptor<LocalDate>()
      verify(performanceDataRepository).findByWeekBetween(any(), ltCaptor.capture())
      assertThat(ltCaptor.firstValue).isEqualTo(today)
    }
  }

  // -------------------------------------------------------------------------
  // PRF-022: Filter by week range (week__gte, week__lt)
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-022 passes explicit week range to repository`() {
    val gte = LocalDate.of(2023, 1, 2)
    val lt = LocalDate.of(2024, 1, 1)
    whenever(performanceDataRepository.findByWeekBetween(gte, lt)).thenReturn(emptyList())

    service.getPerformanceData(weekGte = gte, weekLt = lt)

    verify(performanceDataRepository).findByWeekBetween(gte, lt)
  }

  // -------------------------------------------------------------------------
  // PRF-023: Returns formatted percentages (e.g. "95%")
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("percentage formatting (PRF-023)")
  inner class PercentageFormatting {

    @Test
    fun `PRF-023 formats digital_takeup as percentage string`() {
      whenever(performanceDataRepository.findByWeekBetween(any(), any()))
        .thenReturn(listOf(makePerformanceData(digitalTakeup = 0.9545)))

      val response = service.getPerformanceData(null, null)

      assertThat(response.results[0].digitalTakeup).isEqualTo("95%")
    }

    @Test
    fun `PRF-023 formats completion_rate as percentage string`() {
      whenever(performanceDataRepository.findByWeekBetween(any(), any()))
        .thenReturn(listOf(makePerformanceData(completionRate = 0.666666)))

      val response = service.getPerformanceData(null, null)

      assertThat(response.results[0].completionRate).isEqualTo("67%")
    }

    @Test
    fun `PRF-023 formats user_satisfaction as percentage string`() {
      whenever(performanceDataRepository.findByWeekBetween(any(), any()))
        .thenReturn(listOf(makePerformanceData(userSatisfaction = 0.5)))

      val response = service.getPerformanceData(null, null)

      assertThat(response.results[0].userSatisfaction).isEqualTo("50%")
    }

    @Test
    fun `PRF-023 leaves null percentage fields as null`() {
      whenever(performanceDataRepository.findByWeekBetween(any(), any()))
        .thenReturn(listOf(makePerformanceData(digitalTakeup = null, completionRate = null, userSatisfaction = null)))

      val response = service.getPerformanceData(null, null)

      assertThat(response.results[0].digitalTakeup).isNull()
      assertThat(response.results[0].completionRate).isNull()
      assertThat(response.results[0].userSatisfaction).isNull()
    }

    @Test
    fun `PRF-023 does not format non-percentage fields`() {
      whenever(performanceDataRepository.findByWeekBetween(any(), any()))
        .thenReturn(listOf(makePerformanceData(creditsTotal = 150, creditsByMtp = 120)))

      val response = service.getPerformanceData(null, null)

      assertThat(response.results[0].creditsTotal).isEqualTo(150)
      assertThat(response.results[0].creditsByMtp).isEqualTo(120)
    }
  }

  // -------------------------------------------------------------------------
  // PRF-024: Headers response for CSV
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-024 response includes headers map for CSV export`() {
    whenever(performanceDataRepository.findByWeekBetween(any(), any())).thenReturn(emptyList())

    val response = service.getPerformanceData(null, null)

    assertThat(response.headers).isNotEmpty
    assertThat(response.headers).containsKey("week")
    assertThat(response.headers).containsKey("credits_total")
    assertThat(response.headers).containsKey("digital_takeup")
    assertThat(response.headers).containsKey("completion_rate")
    assertThat(response.headers).containsKey("user_satisfaction")
  }

  @Test
  fun `PRF-024 headers contain verbose names`() {
    whenever(performanceDataRepository.findByWeekBetween(any(), any())).thenReturn(emptyList())

    val response = service.getPerformanceData(null, null)

    assertThat(response.headers["credits_total"]).isEqualTo("credits total")
    assertThat(response.headers["digital_takeup"]).isEqualTo("digital takeup")
    assertThat(response.headers["completion_rate"]).isEqualTo("completion rate")
    assertThat(response.headers["user_satisfaction"]).isEqualTo("user satisfaction")
  }
}
