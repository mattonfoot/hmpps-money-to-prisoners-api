package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(name = "prison_population", schema = "public")
open class PrisonPopulation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 30)
  @NotNull
  @Column(name = "name", nullable = false, length = 30)
  open var name: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "description", nullable = false)
  open var description: String = ""
}
