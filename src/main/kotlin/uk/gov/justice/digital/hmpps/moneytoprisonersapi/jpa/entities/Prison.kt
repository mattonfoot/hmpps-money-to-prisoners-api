package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "prison_prison")
class Prison(
  @Id
  @Column(name = "nomis_id", length = 10)
  val nomisId: String,

  @Column(nullable = false)
  var name: String = "",

  @Column(nullable = false)
  var region: String = "",

  @Column(name = "pre_approval_required", nullable = false)
  var preApprovalRequired: Boolean = false,

  @Column(name = "private_estate", nullable = false)
  var privateEstate: Boolean = false,

  @Column(name = "use_nomis_for_balances", nullable = false)
  var useNomisForBalances: Boolean = true,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,

  @Column(nullable = false)
  var modified: LocalDateTime? = null,
) {

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "prison_prison_categories",
    joinColumns = [JoinColumn(name = "prison_nomis_id")],
    inverseJoinColumns = [JoinColumn(name = "category_id")],
  )
  var categories: MutableSet<PrisonCategory> = mutableSetOf()

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "prison_prison_populations",
    joinColumns = [JoinColumn(name = "prison_nomis_id")],
    inverseJoinColumns = [JoinColumn(name = "population_id")],
  )
  var populations: MutableSet<PrisonPopulation> = mutableSetOf()

  @PrePersist
  fun onCreate() {
    val now = LocalDateTime.now()
    created = now
    modified = now
  }

  @PreUpdate
  fun onUpdate() {
    modified = LocalDateTime.now()
  }
}
