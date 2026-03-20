package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DigitalTakeupRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("DigitalTakeupService")
class DigitalTakeupServiceTest {

  @Mock
  private lateinit var digitalTakeupRepository: DigitalTakeupRepository

  @InjectMocks
  private lateinit var service: DigitalTakeupService

  // -------------------------------------------------------------------------
  // PRF-004: Per-month aggregation across prison set
  // PRF-006: Can exclude private estate
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("digitalTakeupPerMonth (PRF-004, PRF-006)")
  inner class DigitalTakeupPerMonth {

    @Test
    fun `PRF-004 delegates to repository with correct parameters`() {
      whenever(digitalTakeupRepository.digitalTakeupPerMonth(false)).thenReturn(emptyList())

      service.digitalTakeupPerMonth(excludePrivateEstate = false)

      verify(digitalTakeupRepository).digitalTakeupPerMonth(false)
    }

    @Test
    fun `PRF-006 passes excludePrivateEstate=true to repository`() {
      whenever(digitalTakeupRepository.digitalTakeupPerMonth(true)).thenReturn(emptyList())

      service.digitalTakeupPerMonth(excludePrivateEstate = true)

      verify(digitalTakeupRepository).digitalTakeupPerMonth(true)
    }

    @Test
    fun `PRF-004 computes digital takeup for each month from projection totals`() {
      val projections = listOf(
        makeProjection("2024-01", totalByPost = 30, totalByMtp = 70),
        makeProjection("2024-02", totalByPost = 0, totalByMtp = 0),
      )
      whenever(digitalTakeupRepository.digitalTakeupPerMonth(false)).thenReturn(projections)

      val result = service.digitalTakeupPerMonth(excludePrivateEstate = false)

      assertThat(result).hasSize(2)
      assertThat(result[0].month).isEqualTo("2024-01")
      assertThat(result[0].creditsByPost).isEqualTo(30L)
      assertThat(result[0].creditsByMtp).isEqualTo(70L)
      assertThat(result[0].digitalTakeup).isEqualTo(0.7)
      assertThat(result[1].digitalTakeup).isNull()
    }
  }

  // -------------------------------------------------------------------------
  // PRF-005: Mean digital takeup — average across queryset
  // PRF-006: Can exclude private estate
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("meanDigitalTakeup (PRF-005, PRF-006)")
  inner class MeanDigitalTakeup {

    @Test
    fun `PRF-005 delegates to repository for mean calculation`() {
      whenever(digitalTakeupRepository.meanDigitalTakeup(false)).thenReturn(0.75)

      val result = service.meanDigitalTakeup(excludePrivateEstate = false)

      assertThat(result).isEqualTo(0.75)
      verify(digitalTakeupRepository).meanDigitalTakeup(false)
    }

    @Test
    fun `PRF-006 passes excludePrivateEstate=true for mean`() {
      whenever(digitalTakeupRepository.meanDigitalTakeup(true)).thenReturn(0.85)

      val result = service.meanDigitalTakeup(excludePrivateEstate = true)

      assertThat(result).isEqualTo(0.85)
      verify(digitalTakeupRepository).meanDigitalTakeup(true)
    }

    @Test
    fun `PRF-005 returns null when no data`() {
      whenever(digitalTakeupRepository.meanDigitalTakeup(false)).thenReturn(null)

      val result = service.meanDigitalTakeup(excludePrivateEstate = false)

      assertThat(result).isNull()
    }
  }

  private fun makeProjection(month: String, totalByPost: Long, totalByMtp: Long): MonthlyTakeupProjection = object : MonthlyTakeupProjection {
    override fun getMonth(): String = month
    override fun getTotalCreditsByPost(): Long = totalByPost
    override fun getTotalCreditsByMtp(): Long = totalByMtp
  }
}
