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
  name = "notification_prisonerprofileevent",
  schema = "public",
  indexes = [
    Index(
      name = "notification_prisonerprofileevent_prisoner_profile_id_3b3b1523",
      columnList = "prisoner_profile_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "notification_prisonerprofileevent_event_id_key",
      columnNames = ["event_id"],
    ),
  ],
)
open class NotificationPrisonerprofileevent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  open var event: NotificationEvent? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prisoner_profile_id", nullable = false)
  open var prisonerProfile: SecurityPrisonerprofile? =
    null
}
