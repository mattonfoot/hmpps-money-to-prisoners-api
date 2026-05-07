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
  name = "security_prisonerprofile_prisons",
  schema = "public",
  indexes = [
    Index(
      name = "security_prisonerprofile_prisons_prisonerprofile_id_97f6c8b0",
      columnList = "prisonerprofile_id",
    ),
    Index(
      name = "security_prisonerprofile_prisons_prison_id_3fb21af7",
      columnList = "prison_id",
    ),
    Index(
      name = "security_prisonerprofile_prisons_prison_id_3fb21af7_like",
      columnList = "prison_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_prisonerprofile_prisonerprofile_id_priso_73b5e1b4_uniq",
      columnNames = [
        "prisonerprofile_id",
        "prison_id",
      ],
    ),
  ],
)
open class SecurityPrisonerprofilePrison {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prisonerprofile_id", nullable = false)
  open var prisonerprofile: SecurityPrisonerprofile? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null
}
