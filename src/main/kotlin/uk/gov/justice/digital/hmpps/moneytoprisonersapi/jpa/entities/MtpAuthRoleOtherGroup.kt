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
  name = "mtp_auth_role_other_groups",
  schema = "public",
  indexes = [
    Index(
      name = "mtp_auth_role_other_groups_role_id_fabf52a0",
      columnList = "role_id",
    ),
    Index(
      name = "mtp_auth_role_other_groups_group_id_f69f82cd",
      columnList = "group_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "mtp_auth_role_other_groups_role_id_group_id_18ed3381_uniq",
      columnNames = [
        "role_id",
        "group_id",
      ],
    ),
  ],
)
open class MtpAuthRoleOtherGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  open var role: MtpAuthRole? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  open var group: AuthGroup? = null
}
