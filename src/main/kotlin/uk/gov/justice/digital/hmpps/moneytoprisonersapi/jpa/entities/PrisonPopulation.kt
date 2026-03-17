package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "prison_populations")
class PrisonPopulation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "population_id")
  val id: Long? = null,

  @Column(nullable = false, unique = true)
  val name: String,
)
