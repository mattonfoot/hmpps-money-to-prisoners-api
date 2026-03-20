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
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "mtp_users")
class MtpUser(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  /** Case-insensitive unique username (stored lowercase) */
  @Column(nullable = false, unique = true, length = 150)
  var username: String,

  @Column(nullable = false, length = 254)
  var email: String = "",

  @Column(name = "first_name", nullable = false, length = 150)
  var firstName: String = "",

  @Column(name = "last_name", nullable = false, length = 150)
  var lastName: String = "",

  @Column(name = "is_active", nullable = false)
  var isActive: Boolean = true,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id")
  var role: MtpRole? = null,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,

  @Column(nullable = false)
  var modified: LocalDateTime? = null,
) {

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "mtp_user_prisons",
    joinColumns = [JoinColumn(name = "user_id")],
    inverseJoinColumns = [JoinColumn(name = "prison_nomis_id")],
  )
  var prisons: MutableSet<Prison> = mutableSetOf()

  @PrePersist
  fun onCreate() {
    val now = LocalDateTime.now()
    created = now
    modified = now
  }

  @PreUpdate
  fun onUpdate() {
    modified = LocalDateTime.now()
  }
}
