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
  name = "security_prisonerprofile_recipients",
  schema = "public",
  indexes = [
    Index(
      name = "security_prisonerprofile_recipients_prisonerprofile_id_218638d0",
      columnList = "prisonerprofile_id",
    ),
    Index(
      name = "security_prisonerprofile_r_recipientprofile_id_2b9ccd0c",
      columnList = "recipientprofile_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_prisonerprofile_prisonerprofile_id_recip_43fa932f_uniq",
      columnNames = [
        "prisonerprofile_id",
        "recipientprofile_id",
      ],
    ),
  ],
)
open class SecurityPrisonerprofileRecipient {
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
  @JoinColumn(name = "recipientprofile_id", nullable = false)
  open var recipientprofile: SecurityRecipientprofile? =
    null
}
