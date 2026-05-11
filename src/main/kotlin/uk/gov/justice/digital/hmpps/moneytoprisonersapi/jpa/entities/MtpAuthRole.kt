package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "mtp_auth_role",
  schema = "public",
  indexes = [
    Index(
      name = "mtp_auth_role_name_795a081b_like",
      columnList = "name",
    ),
    Index(
      name = "mtp_auth_role_application_id_94ba99f4",
      columnList = "application_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "mtp_auth_role_name_key",
      columnNames = ["name"],
    ),
    UniqueConstraint(
      name = "mtp_auth_role_key_group_id_key",
      columnNames = ["key_group_id"],
    ),
  ],
)
open class MtpAuthRole {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 30)
  @NotNull
  @Column(name = "name", nullable = false, length = 30)
  open var name: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  open var application: Oauth2ProviderApplication? = null

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "key_group_id", nullable = false)
  open var keyGroup: AuthGroup? = null

  @Size(max = 200)
  @NotNull
  @Column(name = "login_url", nullable = false, length = 200)
  open var loginUrl: String = ""

  // Django M2M: mtp_auth_role_other_groups joins role_id ↔ group_id.
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "mtp_auth_role_other_groups",
    joinColumns = [JoinColumn(name = "role_id")],
    inverseJoinColumns = [JoinColumn(name = "group_id")],
  )
  open var otherGroups: MutableSet<AuthGroup> = mutableSetOf()

  /**
   * Transient override used by unit-test factories where the caller passes a
   * comma-separated string ("Viewer,Commenter") rather than persisted
   * AuthGroup rows. When set, DTOs prefer this string verbatim; otherwise
   * they derive a CSV from [otherGroups].
   */
  @Transient
  open var otherGroupsCsv: String? = null
}
