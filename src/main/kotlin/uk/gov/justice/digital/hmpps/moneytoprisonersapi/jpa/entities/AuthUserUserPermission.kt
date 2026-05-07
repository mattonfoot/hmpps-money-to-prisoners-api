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
  name = "auth_user_user_permissions",
  schema = "public",
  indexes = [
    Index(
      name = "auth_user_user_permissions_user_id_a95ead1b",
      columnList = "user_id",
    ),
    Index(
      name = "auth_user_user_permissions_permission_id_1fbb5f2c",
      columnList = "permission_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "auth_user_user_permissions_user_id_permission_id_14a6b632_uniq",
      columnNames = [
        "user_id",
        "permission_id",
      ],
    ),
  ],
)
open class AuthUserUserPermission {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "permission_id", nullable = false)
  open var permission: AuthPermission? = null
}
