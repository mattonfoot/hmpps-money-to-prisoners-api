package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class AccountRequestStatus(val value: String) {
  PENDING("pending"),
  ACCEPTED("accepted"),
  REJECTED("rejected"),
}

@Entity
@Table(name = "mtp_auth_accountrequest")
class AccountRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false, length = 150)
  val username: String,

  @Column(name = "first_name", nullable = false, length = 150)
  var firstName: String = "",

  @Column(name = "last_name", nullable = false, length = 150)
  var lastName: String = "",

  @Column(nullable = false, length = 254)
  var email: String = "",

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id")
  var role: MtpRole? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prison_nomis_id")
  var prison: Prison? = null,

  @Column(nullable = false, length = 20)
  var status: String = AccountRequestStatus.PENDING.value,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,

  @Column(nullable = false)
  var modified: LocalDateTime? = null,
) {
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
