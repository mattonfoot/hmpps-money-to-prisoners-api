package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Entity
@Table(
  name = "performance_digitaltakeup",
  schema = "public",
  indexes = [
    Index(
      name = "performance_digitaltakeup_prison_id_3066ccee",
      columnList = "prison_id",
    ),
    Index(
      name = "performance_digitaltakeup_prison_id_3066ccee_like",
      columnList = "prison_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "performance_digitaltakeup_date_prison_id_d15b43f5_uniq",
      columnNames = [
        "date",
        "prison_id",
      ],
    ),
  ],
)
open class PerformanceDigitaltakeup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @Column(name = "date", nullable = false)
  open var date: LocalDate = LocalDate.now()

  @NotNull
  @Column(name = "credits_by_post", nullable = false)
  open var creditsByPost: Int = 0

  @NotNull
  @Column(name = "credits_by_mtp", nullable = false)
  open var creditsByMtp: Int = 0

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null

  @Column(name = "amount_by_mtp")
  open var amountByMtp: Int? = null

  @Column(name = "amount_by_post")
  open var amountByPost: Int? = null
}
