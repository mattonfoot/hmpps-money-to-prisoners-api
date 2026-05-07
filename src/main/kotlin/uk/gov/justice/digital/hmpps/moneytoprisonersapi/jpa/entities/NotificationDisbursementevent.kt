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
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull

@Entity
@Table(
  name = "notification_disbursementevent",
  schema = "public",
  indexes = [
    Index(
      name = "notification_disbursementevent_disbursement_id_b25439fe",
      columnList = "disbursement_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "notification_disbursementevent_event_id_key",
      columnNames = ["event_id"],
    ),
  ],
)
open class NotificationDisbursementevent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "disbursement_id", nullable = false)
  open var disbursement: DisbursementDisbursement? = null

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  open var event: NotificationEvent? = null
}
