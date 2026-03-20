package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * Weekly performance summary.
 *
 * PRF-020: Served by GET /performance/data/.
 * PRF-023: digitalTakeup, completionRate and userSatisfaction are stored as [0,1] floats
 *          and formatted to percentage strings in the API response.
 */
@Entity
@Table(name = "performance_performancedata")
class PerformanceData(
  /** Primary key — must be a Monday. */
  @Id
  val week: LocalDate,

  @Column(name = "credits_total")
  val creditsTotal: Int? = null,

  @Column(name = "credits_by_mtp")
  val creditsByMtp: Int? = null,

  /** Stored as a [0,1] float; formatted as "N%" in the API response (PRF-023). */
  @Column(name = "digital_takeup")
  val digitalTakeup: Double? = null,

  /** Stored as a [0,1] float; formatted as "N%" in the API response (PRF-023). */
  @Column(name = "completion_rate")
  val completionRate: Double? = null,

  /** Stored as a [0,1] float; formatted as "N%" in the API response (PRF-023). */
  @Column(name = "user_satisfaction")
  val userSatisfaction: Double? = null,

  @Column(name = "rated_1")
  val rated1: Int? = null,

  @Column(name = "rated_2")
  val rated2: Int? = null,

  @Column(name = "rated_3")
  val rated3: Int? = null,

  @Column(name = "rated_4")
  val rated4: Int? = null,

  @Column(name = "rated_5")
  val rated5: Int? = null,
) {
  companion object {
    /** PRF-024: Headers map used in the CSV-friendly API response. */
    val HEADERS: Map<String, String> = linkedMapOf(
      "week" to "week",
      "credits_total" to "credits total",
      "credits_by_mtp" to "credits by mtp",
      "digital_takeup" to "digital takeup",
      "completion_rate" to "completion rate",
      "user_satisfaction" to "user satisfaction",
      "rated_1" to "rated 1",
      "rated_2" to "rated 2",
      "rated_3" to "rated 3",
      "rated_4" to "rated 4",
      "rated_5" to "rated 5",
    )
  }
}
