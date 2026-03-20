package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserSatisfaction
import java.time.LocalDate

interface UserSatisfactionRepository : JpaRepository<UserSatisfaction, LocalDate> {

  /**
   * PRF-013: Mean percentage satisfied across all records.
   * Returns null when there are no records with non-zero totals.
   */
  @Query(
    nativeQuery = true,
    value = """
      SELECT AVG(
        CASE WHEN (rated_1 + rated_2 + rated_3 + rated_4 + rated_5) > 0
             THEN CAST((rated_4 + rated_5) AS DECIMAL)
                  / (rated_1 + rated_2 + rated_3 + rated_4 + rated_5)
             ELSE NULL
        END
      )
        FROM performance_usersatisfaction
    """,
  )
  fun meanPercentageSatisfied(): Double?
}
