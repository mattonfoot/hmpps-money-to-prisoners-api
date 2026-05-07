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
  name = "security_senderprofile_prisons",
  schema = "public",
  indexes = [
    Index(
      name = "security_senderprofile_prisons_senderprofile_id_31cc32b4",
      columnList = "senderprofile_id",
    ),
    Index(
      name = "security_senderprofile_prisons_prison_id_52d2fe94",
      columnList = "prison_id",
    ),
    Index(
      name = "security_senderprofile_prisons_prison_id_52d2fe94_like",
      columnList = "prison_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_senderprofile_p_senderprofile_id_prison__9c7fc309_uniq",
      columnNames = [
        "senderprofile_id",
        "prison_id",
      ],
    ),
  ],
)
open class SecuritySenderprofilePrison {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "senderprofile_id", nullable = false)
  open var senderprofile: SecuritySenderprofile? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null
}
