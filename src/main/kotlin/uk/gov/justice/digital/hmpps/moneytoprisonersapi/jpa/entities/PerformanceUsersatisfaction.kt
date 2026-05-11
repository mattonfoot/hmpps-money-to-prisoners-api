package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Entity
@Table(name = "performance_usersatisfaction", schema = "public")
open class PerformanceUsersatisfaction {
  @Id
  @Column(name = "date", nullable = false)
  open var id: LocalDate = LocalDate.now()

  @NotNull
  @Column(name = "rated_1", nullable = false)
  open var rated1: Int = 0

  @NotNull
  @Column(name = "rated_2", nullable = false)
  open var rated2: Int = 0

  @NotNull
  @Column(name = "rated_3", nullable = false)
  open var rated3: Int = 0

  @NotNull
  @Column(name = "rated_4", nullable = false)
  open var rated4: Int = 0

  @NotNull
  @Column(name = "rated_5", nullable = false)
  open var rated5: Int = 0

  /** Convenience alias for the @Id field — Django models the PK as `date`. */
  @get:jakarta.persistence.Transient
  val date: LocalDate get() = id

  /** PRF-013: sum of all ratings on this day. */
  @get:jakarta.persistence.Transient
  val total: Int get() = rated1 + rated2 + rated3 + rated4 + rated5

  /**
   * PRF-012: ratio of ratings >= 4 over the day's total.
   * Null when there were no ratings — mirrors Django's behaviour on the proxy.
   */
  @get:jakarta.persistence.Transient
  val percentageSatisfied: Double?
    get() {
      val t = total
      return if (t == 0) null else (rated4 + rated5).toDouble() / t.toDouble()
    }
}
