package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PerformanceData
import java.time.LocalDate

interface PerformanceDataRepository : JpaRepository<PerformanceData, LocalDate> {

  /**
   * PRF-022: Returns records where week >= weekGte and week < weekLt, ordered by week.
   */
  @Query("SELECT p FROM PerformancePerformancedatum p WHERE p.id >= :weekGte AND p.id < :weekLt ORDER BY p.id")
  fun findByIdBetween(
    @Param("weekGte") weekGte: LocalDate,
    @Param("weekLt") weekLt: LocalDate,
  ): List<PerformanceData>
}
