package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ServiceStatusDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DowntimeRepository
import java.time.LocalDateTime

/** All known service identifiers (mirrors the Python Service TextChoices). */
private val SERVICES = listOf("gov_uk_pay")

@Service
class ServiceAvailabilityService(
  private val downtimeRepository: DowntimeRepository,
) {

  /**
   * SVC-002: Builds a status map keyed by service name.
   * SVC-003: Appends a wildcard "*" entry that is true only when every service is up.
   * SVC-004: Adds downtime_end and message_to_users when a downtime has those fields.
   * SVC-005: Ongoing downtime (end=null) results in no downtime_end in the response.
   */
  @Transactional(readOnly = true)
  fun getServiceAvailability(): Map<String, ServiceStatusDto> {
    val now = LocalDateTime.now()
    val result = linkedMapOf<String, ServiceStatusDto>()

    for (serviceName in SERVICES) {
      val activeDowntimes = downtimeRepository.findActiveDowntimes(serviceName, now)
      // Prefer ongoing downtimes (null end), then pick the latest-ending one
      val downtime = activeDowntimes.firstOrNull { it.end == null }
        ?: activeDowntimes.maxByOrNull { it.end!! }

      result[serviceName] = if (downtime == null) {
        ServiceStatusDto(status = true)
      } else {
        ServiceStatusDto(
          status = false,
          downtimeEnd = downtime.end?.toString(),
          messageToUsers = downtime.messageToUsers.takeIf { it.isNotBlank() },
        )
      }
    }

    result["*"] = ServiceStatusDto(status = result.values.all { it.status })
    return result
  }
}
