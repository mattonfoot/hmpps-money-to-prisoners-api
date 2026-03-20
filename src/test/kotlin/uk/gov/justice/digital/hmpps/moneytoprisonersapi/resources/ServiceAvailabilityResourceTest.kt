package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ServiceStatusDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.ServiceAvailabilityService

@ExtendWith(MockitoExtension::class)
@DisplayName("ServiceAvailabilityResource")
class ServiceAvailabilityResourceTest {

  @Mock
  private lateinit var serviceAvailabilityService: ServiceAvailabilityService

  @InjectMocks
  private lateinit var resource: ServiceAvailabilityResource

  private fun statusMap(govUkPayUp: Boolean = true) = mapOf(
    "gov_uk_pay" to ServiceStatusDto(status = govUkPayUp),
    "*" to ServiceStatusDto(status = govUkPayUp),
  )

  // -------------------------------------------------------------------------
  // SVC-001: GET /service-availability/ is public (tested via config, not here)
  // SVC-002: Returns status per service
  // SVC-003: Includes wildcard (*) status
  // -------------------------------------------------------------------------

  @Test
  fun `SVC-001 delegates to service and returns its result`() {
    val expected = statusMap(true)
    whenever(serviceAvailabilityService.getServiceAvailability()).thenReturn(expected)

    val result = resource.getServiceAvailability()

    assertThat(result).isEqualTo(expected)
    verify(serviceAvailabilityService).getServiceAvailability()
  }

  @Test
  fun `SVC-002 result contains gov_uk_pay key`() {
    whenever(serviceAvailabilityService.getServiceAvailability()).thenReturn(statusMap())

    val result = resource.getServiceAvailability()

    assertThat(result).containsKey("gov_uk_pay")
  }

  @Test
  fun `SVC-003 result contains wildcard key`() {
    whenever(serviceAvailabilityService.getServiceAvailability()).thenReturn(statusMap())

    val result = resource.getServiceAvailability()

    assertThat(result).containsKey("*")
  }

  @Test
  fun `SVC-002 gov_uk_pay status reflects service status`() {
    whenever(serviceAvailabilityService.getServiceAvailability()).thenReturn(statusMap(false))

    val result = resource.getServiceAvailability()

    assertThat(result["gov_uk_pay"]?.status).isFalse()
  }
}
