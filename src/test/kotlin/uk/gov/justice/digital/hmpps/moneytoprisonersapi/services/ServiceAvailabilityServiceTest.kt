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
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Downtime
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DowntimeRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("ServiceAvailabilityService")
class ServiceAvailabilityServiceTest {

  @Mock
  private lateinit var downtimeRepository: DowntimeRepository

  @InjectMocks
  private lateinit var service: ServiceAvailabilityService

  private val pastTime = LocalDateTime.now().minusHours(1)
  private val futureTime = LocalDateTime.now().plusHours(1)

  // -------------------------------------------------------------------------
  // SVC-002: Returns status per service
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("status per service (SVC-002)")
  inner class StatusPerService {

    @Test
    fun `SVC-002 returns true when no active downtime`() {
      whenever(downtimeRepository.findActiveDowntimes(eq("gov_uk_pay"), any())).thenReturn(emptyList())

      val result = service.getServiceAvailability()

      assertThat(result["gov_uk_pay"]?.status).isTrue()
    }

    @Test
    fun `SVC-002 returns false when active downtime exists`() {
      val downtime = Downtime(service = "gov_uk_pay", start = pastTime)
      whenever(downtimeRepository.findActiveDowntimes(eq("gov_uk_pay"), any())).thenReturn(listOf(downtime))

      val result = service.getServiceAvailability()

      assertThat(result["gov_uk_pay"]?.status).isFalse()
    }
  }

  // -------------------------------------------------------------------------
  // SVC-003: Includes wildcard (*) status
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("wildcard status (SVC-003)")
  inner class WildcardStatus {

    @Test
    fun `SVC-003 wildcard is true when all services are up`() {
      whenever(downtimeRepository.findActiveDowntimes(any(), any())).thenReturn(emptyList())

      val result = service.getServiceAvailability()

      assertThat(result["*"]?.status).isTrue()
    }

    @Test
    fun `SVC-003 wildcard is false when any service is down`() {
      val downtime = Downtime(service = "gov_uk_pay", start = pastTime)
      whenever(downtimeRepository.findActiveDowntimes(eq("gov_uk_pay"), any())).thenReturn(listOf(downtime))

      val result = service.getServiceAvailability()

      assertThat(result["*"]?.status).isFalse()
    }

    @Test
    fun `SVC-003 wildcard has only status field`() {
      whenever(downtimeRepository.findActiveDowntimes(any(), any())).thenReturn(emptyList())

      val result = service.getServiceAvailability()

      assertThat(result["*"]?.downtimeEnd).isNull()
      assertThat(result["*"]?.messageToUsers).isNull()
    }
  }

  // -------------------------------------------------------------------------
  // SVC-004: Active downtime includes downtime_end and message_to_users
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("active downtime fields (SVC-004)")
  inner class ActiveDowntimeFields {

    @Test
    fun `SVC-004 includes downtime_end when end is set`() {
      val downtime = Downtime(service = "gov_uk_pay", start = pastTime, end = futureTime)
      whenever(downtimeRepository.findActiveDowntimes(eq("gov_uk_pay"), any())).thenReturn(listOf(downtime))

      val result = service.getServiceAvailability()

      assertThat(result["gov_uk_pay"]?.downtimeEnd).isNotNull()
    }

    @Test
    fun `SVC-004 does not include downtime_end when end is null`() {
      val downtime = Downtime(service = "gov_uk_pay", start = pastTime, end = null)
      whenever(downtimeRepository.findActiveDowntimes(eq("gov_uk_pay"), any())).thenReturn(listOf(downtime))

      val result = service.getServiceAvailability()

      assertThat(result["gov_uk_pay"]?.downtimeEnd).isNull()
    }

    @Test
    fun `SVC-004 includes message_to_users when set`() {
      val downtime = Downtime(service = "gov_uk_pay", start = pastTime, messageToUsers = "Maintenance in progress")
      whenever(downtimeRepository.findActiveDowntimes(eq("gov_uk_pay"), any())).thenReturn(listOf(downtime))

      val result = service.getServiceAvailability()

      assertThat(result["gov_uk_pay"]?.messageToUsers).isEqualTo("Maintenance in progress")
    }

    @Test
    fun `SVC-004 does not include message_to_users when empty`() {
      val downtime = Downtime(service = "gov_uk_pay", start = pastTime, messageToUsers = "")
      whenever(downtimeRepository.findActiveDowntimes(eq("gov_uk_pay"), any())).thenReturn(listOf(downtime))

      val result = service.getServiceAvailability()

      assertThat(result["gov_uk_pay"]?.messageToUsers).isNull()
    }
  }

  // -------------------------------------------------------------------------
  // SVC-005: Null end = ongoing downtime
  // -------------------------------------------------------------------------

  @Test
  fun `SVC-005 null end means ongoing downtime with no downtime_end in response`() {
    val downtime = Downtime(service = "gov_uk_pay", start = pastTime, end = null)
    whenever(downtimeRepository.findActiveDowntimes(eq("gov_uk_pay"), any())).thenReturn(listOf(downtime))

    val result = service.getServiceAvailability()

    assertThat(result["gov_uk_pay"]?.status).isFalse()
    assertThat(result["gov_uk_pay"]?.downtimeEnd).isNull()
  }
}
