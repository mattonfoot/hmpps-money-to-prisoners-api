package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "security_checkautoacceptrule")
class AutoAcceptRule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_profile_id", nullable = false)
  var senderProfile: SenderProfile,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prisoner_profile_id", nullable = false)
  var prisonerProfile: PrisonerProfile,

  @OneToMany(mappedBy = "rule", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @OrderBy("created ASC")
  var states: MutableList<AutoAcceptRuleState> = mutableListOf(),

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

  fun isActive(): Boolean = states.lastOrNull()?.active ?: false
}
