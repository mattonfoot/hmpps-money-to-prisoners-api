package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ServiceNotificationDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.ServiceNotificationRepository
import java.time.LocalDateTime

@Service
class ServiceNotificationService(
  private val serviceNotificationRepository: ServiceNotificationRepository,
) {

  /**
   * SVC-010: Returns active notifications (now between start and end).
   * SVC-011: Unauthenticated users only see public=true notifications.
   * SVC-012: Optionally filters by target prefix.
   * SVC-013: Level is mapped to a string label in the DTO.
   */
  @Transactional(readOnly = true)
  fun listNotifications(authenticated: Boolean, targetPrefix: String?): List<ServiceNotificationDto> {
    val now = LocalDateTime.now()
    var notifications = serviceNotificationRepository.findActive(now)

    if (!authenticated) {
      notifications = notifications.filter { it.public }
    }

    if (targetPrefix != null) {
      notifications = notifications.filter { it.target.startsWith(targetPrefix) }
    }

    return notifications.map { ServiceNotificationDto.from(it) }
  }
}
