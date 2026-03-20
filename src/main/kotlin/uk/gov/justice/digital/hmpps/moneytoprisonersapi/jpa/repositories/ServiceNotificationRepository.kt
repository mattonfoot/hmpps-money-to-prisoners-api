package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.ServiceNotification
import java.time.LocalDateTime

interface ServiceNotificationRepository : JpaRepository<ServiceNotification, Long> {

  /**
   * Returns notifications that have started and have not yet ended.
   * Active = start <= now AND (end IS NULL OR end >= now).
   * SVC-014: time-windowed visibility.
   */
  @Query(
    """
    SELECT n FROM ServiceNotification n
    WHERE n.start <= :now
      AND (n.end IS NULL OR n.end >= :now)
    """,
  )
  fun findActive(@Param("now") now: LocalDateTime): List<ServiceNotification>
}
