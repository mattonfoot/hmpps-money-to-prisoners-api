package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Downtime
import java.time.LocalDateTime

interface DowntimeRepository : JpaRepository<Downtime, Long> {

  /**
   * Returns all active downtime records for a service.
   * Active = start <= now AND (end > now OR end IS NULL).
   * SVC-005: null end means ongoing downtime.
   */
  @Query(
    """
    SELECT d FROM Downtime d
    WHERE d.service = :service
      AND d.start <= :now
      AND (d.end > :now OR d.end IS NULL)
    """,
  )
  fun findActiveDowntimes(
    @Param("service") service: String,
    @Param("now") now: LocalDateTime,
  ): List<Downtime>
}
