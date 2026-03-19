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
import java.time.LocalDateTime

/**
 * NOT-001: Event has rule code.
 * NOT-002: Events can be user-specific (username set) or global (username null).
 */
@Entity
@Table(
  name = "notification_events",
  indexes = [
    Index(name = "idx_notification_events_triggered_at_id", columnList = "triggered_at DESC, id"),
    Index(name = "idx_notification_events_rule", columnList = "rule"),
  ],
)
class Event(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false, length = 8)
  val rule: String,

  @Column(nullable = false, length = 500)
  val description: String = "",

  @Column(name = "triggered_at")
  val triggeredAt: LocalDateTime? = null,

  /** Null means global event (visible to all users who subscribe to the rule). */
  @Column(name = "username", length = 250)
  val username: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_id", nullable = true)
  val credit: Credit? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "disbursement_id", nullable = true)
  val disbursement: Disbursement? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_profile_id", nullable = true)
  val senderProfile: SenderProfile? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prisoner_profile_id", nullable = true)
  val prisonerProfile: PrisonerProfile? = null,
)
