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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "notification_event",
  schema = "public",
  indexes = [
    Index(
      name = "notificatio_rule_0b334e_idx",
      columnList = "rule",
    ),
    Index(
      name = "notificatio_trigger_ccb935_idx",
      columnList = "triggered_at, id",
    ),
    Index(
      name = "notification_event_user_id_b8316f8b",
      columnList = "user_id",
    ),
  ],
)
open class NotificationEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 8)
  @NotNull
  @Column(name = "rule", nullable = false, length = 8)
  open var rule: String = ""

  @Size(max = 500)
  @NotNull
  @Column(name = "description", nullable = false, length = 500)
  open var description: String = ""

  @Column(name = "triggered_at")
  open var triggeredAt: OffsetDateTime? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  open var user: AuthUser? = null

  // Convenience back-references onto the per-subject child rows. Django stores
  // the actual link on those child tables (notification_creditevent etc.).
  @OneToOne(mappedBy = "event", fetch = FetchType.LAZY)
  open var creditEvent: NotificationCreditevent? = null

  @OneToOne(mappedBy = "event", fetch = FetchType.LAZY)
  open var disbursementEvent: NotificationDisbursementevent? = null

  @OneToOne(mappedBy = "event", fetch = FetchType.LAZY)
  open var senderProfileEvent: NotificationSenderprofileevent? = null

  @OneToOne(mappedBy = "event", fetch = FetchType.LAZY)
  open var prisonerProfileEvent: NotificationPrisonerprofileevent? = null

  @OneToOne(mappedBy = "event", fetch = FetchType.LAZY)
  open var recipientProfileEvent: NotificationRecipientprofileevent? = null

  // Helpers that follow the per-subject child rows, mirroring the legacy
  // Kotlin-domain accessors.
  val credit: CreditCredit? get() = creditEvent?.credit
  val disbursement: DisbursementDisbursement? get() = disbursementEvent?.disbursement
  val senderProfile: SecuritySenderprofile? get() = senderProfileEvent?.senderProfile
  val prisonerProfile: SecurityPrisonerprofile? get() = prisonerProfileEvent?.prisonerProfile
}
