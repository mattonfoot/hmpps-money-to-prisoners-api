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
  name = "auth_group_permissions",
  schema = "public",
  indexes = [
    Index(
      name = "auth_group_permissions_group_id_b120cbf9",
      columnList = "group_id",
    ),
    Index(
      name = "auth_group_permissions_permission_id_84c5c92e",
      columnList = "permission_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "auth_group_permissions_group_id_permission_id_0cd325b0_uniq",
      columnNames = [
        "group_id",
        "permission_id",
      ],
    ),
  ],
)
open class AuthGroupPermission {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id", nullable = false)
  open var group: AuthGroup? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "permission_id", nullable = false)
  open var permission: AuthPermission? = null
}
