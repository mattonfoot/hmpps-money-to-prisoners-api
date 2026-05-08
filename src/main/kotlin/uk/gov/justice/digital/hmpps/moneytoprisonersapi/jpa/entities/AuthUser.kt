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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "auth_user",
  schema = "public",
  indexes = [
    Index(
      name = "auth_user_username_6821ab7c_like",
      columnList = "username",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "auth_user_username_key",
      columnNames = ["username"],
    ),
  ],
)
open class AuthUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 128)
  @NotNull
  @Column(name = "password", nullable = false, length = 128)
  open var password: String = ""

  @Column(name = "last_login")
  open var lastLogin: OffsetDateTime? = null

  @NotNull
  @Column(name = "is_superuser", nullable = false)
  open var isSuperuser: Boolean = false

  @Size(max = 150)
  @NotNull
  @Column(name = "username", nullable = false, length = 150)
  open var username: String = ""

  @Size(max = 150)
  @NotNull
  @Column(name = "first_name", nullable = false, length = 150)
  open var firstName: String = ""

  @Size(max = 150)
  @NotNull
  @Column(name = "last_name", nullable = false, length = 150)
  open var lastName: String = ""

  @Size(max = 254)
  @NotNull
  @Column(name = "email", nullable = false, length = 254)
  open var email: String = ""

  @NotNull
  @Column(name = "is_staff", nullable = false)
  open var isStaff: Boolean = false

  @NotNull
  @Column(name = "is_active", nullable = false)
  open var isActive: Boolean = false

  @NotNull
  @Column(name = "date_joined", nullable = false)
  open var dateJoined: OffsetDateTime = OffsetDateTime.now()

  // Manually-maintained back-reference. Django models prison-mapping via
  // mtp_auth_prisonusermapping (one row per user). This nav lets services
  // reach the user's prisons without going through PrisonUserMappingService.
  @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
  open var prisonUserMapping: MtpAuthPrisonusermapping? = null

  // Convenience: walk through prisonUserMapping. Used by UserService /
  // AccountRequestService where existing code expected `user.prisons`.
  // Setting allocates an unsaved `MtpAuthPrisonusermapping` if needed —
  // services persisting prison changes should still go through
  // `PrisonUserMappingService` to wire up the FK back to this user.
  var prisons: MutableSet<PrisonPrison>
    get() = prisonUserMapping?.prisons ?: mutableSetOf()
    set(value) {
      val mapping = prisonUserMapping ?: MtpAuthPrisonusermapping().also { it.user = this; prisonUserMapping = it }
      mapping.prisons = value
    }

  // Django auth groups M2M (auth_user_groups). Used by the OAuth2 filter to
  // map group memberships to Spring Security authorities.
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "auth_user_groups",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "group_id")],
  )
  open var groups: MutableSet<AuthGroup> = mutableSetOf()

  // Django resolves a user's `role` by joining `mtp_auth_role.key_group_id`
  // through the user's groups. Held transient here as a cached convenience —
  // populated by services that need it. Not a persisted column.
  @Transient
  open var role: MtpAuthRole? = null
}
