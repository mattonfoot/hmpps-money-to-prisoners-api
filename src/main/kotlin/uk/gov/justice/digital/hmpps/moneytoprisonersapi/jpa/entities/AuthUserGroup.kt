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
  name = "auth_user_groups",
  schema = "public",
  indexes = [
    Index(
      name = "auth_user_groups_user_id_6a12ed8b",
      columnList = "user_id",
    ),
    Index(
      name = "auth_user_groups_group_id_97559544",
      columnList = "group_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "auth_user_groups_user_id_group_id_94350c0c_uniq",
      columnNames = [
        "user_id",
        "group_id",
      ],
    ),
  ],
)
open class AuthUserGroup {
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
  @JoinColumn(name = "group_id", nullable = false)
  open var group: AuthGroup? = null
}
