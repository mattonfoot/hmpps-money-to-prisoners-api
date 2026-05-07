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
  name = "prison_prison_populations",
  schema = "public",
  indexes = [
    Index(
      name = "prison_prison_populations_prison_id_a14a8b76",
      columnList = "prison_id",
    ),
    Index(
      name = "prison_prison_populations_prison_id_a14a8b76_like",
      columnList = "prison_id",
    ),
    Index(
      name = "prison_prison_populations_population_id_e23bf37c",
      columnList = "population_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "prison_prison_populations_prison_id_population_id_91c188bf_uniq",
      columnNames = [
        "prison_id",
        "population_id",
      ],
    ),
  ],
)
open class PrisonPrisonPopulation {
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
  @JoinColumn(name = "population_id", nullable = false)
  open var population: PrisonPopulation? = null
}
