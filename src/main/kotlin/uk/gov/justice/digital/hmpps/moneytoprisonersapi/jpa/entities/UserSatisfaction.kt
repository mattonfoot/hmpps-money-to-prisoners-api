package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * PRF-010: Stores daily user satisfaction ratings from 1 (very dissatisfied) to 5 (very satisfied).
 * PRF-011: The date field is the primary key — one record per day.
 * PRF-012: Computed percentageSatisfied = (rated_4 + rated_5) / total.
 */
@Entity
@Table(name = "performance_usersatisfaction")
class UserSatisfaction(
  /** PRF-011: date is the primary key. */
  @Id
  val date: LocalDate,

  @Column(name = "rated_1", nullable = false)
  val rated1: Int = 0,

  @Column(name = "rated_2", nullable = false)
  val rated2: Int = 0,

  @Column(name = "rated_3", nullable = false)
  val rated3: Int = 0,

  @Column(name = "rated_4", nullable = false)
  val rated4: Int = 0,

  @Column(name = "rated_5", nullable = false)
  val rated5: Int = 0,
) {
  val total: Int
    get() = rated1 + rated2 + rated3 + rated4 + rated5

  /** PRF-012: Null when total is zero. */
  val percentageSatisfied: Double?
    get() = if (total == 0) null else (rated4 + rated5).toDouble() / total
}
