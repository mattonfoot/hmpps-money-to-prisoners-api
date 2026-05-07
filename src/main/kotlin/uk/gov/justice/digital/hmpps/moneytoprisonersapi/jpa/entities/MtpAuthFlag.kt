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
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "mtp_auth_flag",
  schema = "public",
  indexes = [
    Index(
      name = "mtp_auth_flag_name_8094cca7",
      columnList = "name",
    ),
    Index(
      name = "mtp_auth_flag_name_8094cca7_like",
      columnList = "name",
    ),
    Index(
      name = "mtp_auth_flag_user_id_166aebbf",
      columnList = "user_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "mtp_auth_flag_user_id_name_35cded76_uniq",
      columnNames = [
        "user_id",
        "name",
      ],
    ),
  ],
)
open class MtpAuthFlag {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 50)
  @NotNull
  @Column(name = "name", nullable = false, length = 50)
  open var name: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}
