package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * NOT-010 to NOT-012: User email notification frequency preference.
 * Supports update-or-create semantics via username unique constraint.
 */
@Entity
@Table(name = "notification_emailnotificationpreferences")
class EmailNotificationPreferences(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false, unique = true, length = 250)
  val username: String,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var frequency: EmailFrequency,

  @Column(name = "last_sent_at")
  var lastSentAt: LocalDate? = null,
)
