package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DigitalTakeup
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.MonthlyTakeupProjection

interface DigitalTakeupRepository : JpaRepository<DigitalTakeup, Long> {

  /**
   * PRF-004: Per-month aggregation across prison set.
   * PRF-006: Optionally excludes private estate prisons.
   */
  @Query(
    nativeQuery = true,
    value = """
      SELECT TO_CHAR(d.date, 'YYYY-MM') AS month,
             SUM(d.credits_by_post)     AS totalCreditsByPost,
             SUM(d.credits_by_mtp)      AS totalCreditsByMtp
        FROM performance_digitaltakeup d
        JOIN prisons p ON p.nomis_id = d.prison_id
       WHERE (:excludePrivateEstate = FALSE OR p.private_estate = FALSE)
       GROUP BY TO_CHAR(d.date, 'YYYY-MM')
       ORDER BY TO_CHAR(d.date, 'YYYY-MM')
    """,
  )
  fun digitalTakeupPerMonth(
    @Param("excludePrivateEstate") excludePrivateEstate: Boolean,
  ): List<MonthlyTakeupProjection>

  /**
   * PRF-005: Mean digital takeup across the (optionally filtered) queryset.
   * PRF-006: Optionally excludes private estate prisons.
   */
  @Query(
    nativeQuery = true,
    value = """
      SELECT AVG(
        CASE WHEN (d.credits_by_post + d.credits_by_mtp) > 0
             THEN CAST(d.credits_by_mtp AS DECIMAL) / (d.credits_by_post + d.credits_by_mtp)
             ELSE NULL
        END
      )
        FROM performance_digitaltakeup d
        JOIN prisons p ON p.nomis_id = d.prison_id
       WHERE (:excludePrivateEstate = FALSE OR p.private_estate = FALSE)
    """,
  )
  fun meanDigitalTakeup(
    @Param("excludePrivateEstate") excludePrivateEstate: Boolean,
  ): Double?
}
