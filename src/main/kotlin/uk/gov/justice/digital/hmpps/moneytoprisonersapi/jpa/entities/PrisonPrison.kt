package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(name = "prison_prison", schema = "public")
open class PrisonPrison {
  @Id
  @Size(max = 3)
  @Column(name = "nomis_id", nullable = false, length = 3)
  open var nomisId: String = ""

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "modified", nullable = false)
  open var modified: OffsetDateTime = OffsetDateTime.now()

  @Size(max = 500)
  @NotNull
  @Column(name = "name", nullable = false, length = 500)
  open var name: String = ""

  @Size(max = 8)
  @NotNull
  @Column(name = "general_ledger_code", nullable = false, length = 8)
  open var generalLedgerCode: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "region", nullable = false)
  open var region: String = ""

  @NotNull
  @Column(name = "pre_approval_required", nullable = false)
  open var preApprovalRequired: Boolean = false

  @Size(max = 10)
  @NotNull
  @Column(name = "cms_establishment_code", nullable = false, length = 10)
  open var cmsEstablishmentCode: String = ""

  @NotNull
  @Column(name = "private_estate", nullable = false)
  open var privateEstate: Boolean = false

  @NotNull
  @Column(name = "use_nomis_for_balances", nullable = false)
  open var useNomisForBalances: Boolean = false

  // Manually-maintained M2M back-references onto Django's prison junction tables.
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "prison_prison_categories",
    schema = "public",
    joinColumns = [JoinColumn(name = "prison_id")],
    inverseJoinColumns = [JoinColumn(name = "category_id")],
  )
  open var categories: MutableSet<PrisonCategory> = mutableSetOf()

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "prison_prison_populations",
    schema = "public",
    joinColumns = [JoinColumn(name = "prison_id")],
    inverseJoinColumns = [JoinColumn(name = "population_id")],
  )
  open var populations: MutableSet<PrisonPopulation> = mutableSetOf()
}
