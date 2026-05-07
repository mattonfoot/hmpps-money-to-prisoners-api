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

@Entity
@Table(
  name = "prison_prison_categories",
  schema = "public",
  indexes = [
    Index(
      name = "prison_prison_categories_prison_id_d47cde4c",
      columnList = "prison_id",
    ),
    Index(
      name = "prison_prison_categories_prison_id_d47cde4c_like",
      columnList = "prison_id",
    ),
    Index(
      name = "prison_prison_categories_category_id_bac6269a",
      columnList = "category_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "prison_prison_categories_prison_id_category_id_3726ef0e_uniq",
      columnNames = [
        "prison_id",
        "category_id",
      ],
    ),
  ],
)
open class PrisonPrisonCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  open var category: PrisonCategory? = null
}
