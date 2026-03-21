package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "security_recipientprofile")
class RecipientProfile(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "recipient_profile_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(name = "sort_code", length = 50)
  var sortCode: String? = null,

  @Column(name = "account_number", length = 50)
  var accountNumber: String? = null,

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "recipient_profile_monitoring_users", joinColumns = [JoinColumn(name = "recipient_profile_id")])
  @Column(name = "user_id")
  var monitoringUsers: MutableSet<String> = mutableSetOf(),

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
