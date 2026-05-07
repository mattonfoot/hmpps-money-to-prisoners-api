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
  name = "security_prisonerprofile_senders",
  schema = "public",
  indexes = [
    Index(
      name = "security_prisonerprofile_senders_prisonerprofile_id_b018d175",
      columnList = "prisonerprofile_id",
    ),
    Index(
      name = "security_prisonerprofile_senders_senderprofile_id_d51aec0d",
      columnList = "senderprofile_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_prisonerprofile_prisonerprofile_id_sende_dfa7db7b_uniq",
      columnNames = [
        "prisonerprofile_id",
        "senderprofile_id",
      ],
    ),
  ],
)
open class SecurityPrisonerprofileSender {
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
  @JoinColumn(name = "senderprofile_id", nullable = false)
  open var senderprofile: SecuritySenderprofile? = null
}
