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
  name = "security_recipientprofile_prisons",
  schema = "public",
  indexes = [
    Index(
      name = "security_recipientprofile_prisons_recipientprofile_id_76e9e709",
      columnList = "recipientprofile_id",
    ),
    Index(
      name = "security_recipientprofile_prisons_prison_id_57707f23",
      columnList = "prison_id",
    ),
    Index(
      name = "security_recipientprofile_prisons_prison_id_57707f23_like",
      columnList = "prison_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_recipientprofil_recipientprofile_id_pris_96404fc5_uniq",
      columnNames = [
        "recipientprofile_id",
        "prison_id",
      ],
    ),
  ],
)
open class SecurityRecipientprofilePrison {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipientprofile_id", nullable = false)
  open var recipientprofile: SecurityRecipientprofile? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null
}
