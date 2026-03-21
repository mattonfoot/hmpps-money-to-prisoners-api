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
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "security_prisonerprofile")
class PrisonerProfile(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "prisoner_profile_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(name = "prisoner_number", length = 250)
  var prisonerNumber: String? = null,

  @Column(name = "prisoner_name", length = 250)
  var prisonerName: String? = null,

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "prisoner_profile_credits",
    joinColumns = [JoinColumn(name = "prisoner_profile_id")],
    inverseJoinColumns = [JoinColumn(name = "credit_id")],
  )
  var credits: MutableSet<Credit> = mutableSetOf(),

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "prisoner_profile_monitoring_users", joinColumns = [JoinColumn(name = "prisoner_profile_id")])
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
