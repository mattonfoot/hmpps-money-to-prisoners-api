package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "auth_user")
class AuthUser(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false, unique = true, length = 150)
  var username: String = "",

  @Column(nullable = false, length = 128)
  var password: String = "",

  @Column(nullable = false, length = 254)
  var email: String = "",

  @Column(name = "first_name", nullable = false, length = 150)
  var firstName: String = "",

  @Column(name = "last_name", nullable = false, length = 150)
  var lastName: String = "",

  @Column(name = "is_staff", nullable = false)
  var isStaff: Boolean = false,

  @Column(name = "is_superuser", nullable = false)
  var isSuperuser: Boolean = false,

  @Column(name = "is_active", nullable = false)
  var isActive: Boolean = true,

  @Column(name = "date_joined", nullable = false)
  var dateJoined: OffsetDateTime = OffsetDateTime.now(),

  @Column(name = "last_login")
  var lastLogin: OffsetDateTime? = null,
) {
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "auth_user_groups",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "group_id")],
  )
  var groups: MutableSet<AuthGroup> = mutableSetOf()
}
