package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Entity
@Table(
  name = "notification_emailnotificationpreferences",
  schema = "public",
  uniqueConstraints = [
    UniqueConstraint(
      name = "notification_emailnotificationpreferences_user_id_key",
      columnNames = ["user_id"],
    ),
  ],
)
open class NotificationEmailnotificationpreference {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 50)
  @NotNull
  @Column(name = "frequency", nullable = false, length = 50)
  open var frequency: String = ""

  @Column(name = "last_sent_at")
  open var lastSentAt: LocalDate? = null

  @NotNull
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}
