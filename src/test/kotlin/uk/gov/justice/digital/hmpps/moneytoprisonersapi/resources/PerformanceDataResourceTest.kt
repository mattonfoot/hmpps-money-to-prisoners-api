package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PerformanceDataDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PerformanceDataResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PerformanceDataService
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("PerformanceDataResource")
class PerformanceDataResourceTest {

  @Mock
  private lateinit var performanceDataService: PerformanceDataService

  @InjectMocks
  private lateinit var resource: PerformanceDataResource

  private val monday = LocalDate.of(2024, 1, 1)

  private fun emptyResponse() = PerformanceDataResponse(
    headers = mapOf("week" to "week"),
    results = emptyList(),
  )

  private fun makeDto(week: LocalDate = monday) = PerformanceDataDto(
    week = week,
    creditsTotal = 100,
    creditsByMtp = 80,
    digitalTakeup = "80%",
    completionRate = "95%",
    userSatisfaction = "67%",
  )

  // -------------------------------------------------------------------------
  // PRF-020: GET /performance/data/ — requires ROLE_SEND_MONEY (tested via annotation)
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-020 delegates to service and returns its response`() {
    val response = PerformanceDataResponse(
      headers = mapOf("week" to "week", "credits_total" to "credits total"),
      results = listOf(makeDto()),
    )
    whenever(performanceDataService.getPerformanceData(null, null)).thenReturn(response)

    val result = resource.getPerformanceData(null, null)

    assertThat(result).isEqualTo(response)
    verify(performanceDataService).getPerformanceData(null, null)
  }

  // -------------------------------------------------------------------------
  // PRF-022: Filter by week range
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("week range filtering (PRF-022)")
  inner class WeekRangeFiltering {

    @Test
    fun `PRF-022 passes week__gte to service`() {
      val gte = LocalDate.of(2023, 6, 5)
      whenever(performanceDataService.getPerformanceData(gte, null)).thenReturn(emptyResponse())

      resource.getPerformanceData(weekGte = gte, weekLt = null)

      verify(performanceDataService).getPerformanceData(gte, null)
    }

    @Test
    fun `PRF-022 passes week__lt to service`() {
      val lt = LocalDate.of(2024, 1, 1)
      whenever(performanceDataService.getPerformanceData(null, lt)).thenReturn(emptyResponse())

      resource.getPerformanceData(weekGte = null, weekLt = lt)

      verify(performanceDataService).getPerformanceData(null, lt)
    }

    @Test
    fun `PRF-022 passes both range bounds to service`() {
      val gte = LocalDate.of(2023, 1, 2)
      val lt = LocalDate.of(2024, 1, 1)
      whenever(performanceDataService.getPerformanceData(gte, lt)).thenReturn(emptyResponse())

      resource.getPerformanceData(weekGte = gte, weekLt = lt)

      verify(performanceDataService).getPerformanceData(gte, lt)
    }
  }

  // -------------------------------------------------------------------------
  // PRF-024: Headers included in response
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-024 response includes headers field`() {
    val headers = mapOf(
      "week" to "week",
      "credits_total" to "credits total",
      "digital_takeup" to "digital takeup",
    )
    whenever(performanceDataService.getPerformanceData(null, null))
      .thenReturn(PerformanceDataResponse(headers = headers, results = emptyList()))

    val result = resource.getPerformanceData(null, null)

    assertThat(result.headers).isEqualTo(headers)
  }

  // -------------------------------------------------------------------------
  // PRF-023: Results contain formatted percentages (end-to-end check at resource level)
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-023 resource passes through pre-formatted percentage strings from service`() {
    val dto = makeDto()
    whenever(performanceDataService.getPerformanceData(null, null))
      .thenReturn(PerformanceDataResponse(headers = emptyMap(), results = listOf(dto)))

    val result = resource.getPerformanceData(null, null)

    assertThat(result.results[0].digitalTakeup).isEqualTo("80%")
    assertThat(result.results[0].completionRate).isEqualTo("95%")
    assertThat(result.results[0].userSatisfaction).isEqualTo("67%")
  }
}
