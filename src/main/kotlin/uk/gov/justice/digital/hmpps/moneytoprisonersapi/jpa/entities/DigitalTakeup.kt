package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * PRF-001: Unique on (date, prison) — enforced by unique constraint.
 * PRF-002: Tracks credits_by_post and credits_by_mtp as integer counts.
 * PRF-003: Computed digitalTakeup = mtp / (post + mtp).
 */
@Entity
@Table(
  name = "performance_digitaltakeup",
  uniqueConstraints = [UniqueConstraint(name = "uq_digitaltakeup_date_prison", columnNames = ["date", "prison_id"])],
)
class DigitalTakeup(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false)
  val date: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prison_id", nullable = false)
  val prison: Prison,

  @Column(name = "credits_by_post", nullable = false)
  val creditsByPost: Int = 0,

  @Column(name = "credits_by_mtp", nullable = false)
  val creditsByMtp: Int = 0,

  @Column(name = "amount_by_post")
  val amountByPost: Int? = null,

  @Column(name = "amount_by_mtp")
  val amountByMtp: Int? = null,
) {
  val creditsTotal: Int
    get() = creditsByPost + creditsByMtp

  /** PRF-003: Returns null when total is zero to avoid division by zero. */
  val digitalTakeup: Double?
    get() = if (creditsTotal == 0) null else creditsByMtp.toDouble() / creditsTotal
}
