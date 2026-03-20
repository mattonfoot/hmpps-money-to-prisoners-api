package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * A banner/alert notification shown to users in one or more front-end applications.
 *
 * SVC-011: public=true makes the notification visible to unauthenticated users.
 * SVC-012: target matches one of the NotificationTarget values; filtered by prefix.
 * SVC-013: level is a Django message integer (20=info, 25=success, 30=warning, 40=error).
 * SVC-014: Active when now is between start and end (or end is null).
 */
@Entity
@Table(name = "service_notification")
class ServiceNotification(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", columnDefinition = "serial")
  val id: Long? = null,

  /** When true, visible to unauthenticated users. */
  @Column(nullable = false)
  val public: Boolean = false,

  /** Target app/view identifier (e.g. "cashbook_login", "noms_ops_security_dashboard"). */
  @Column(nullable = false, length = 30)
  val target: String,

  /**
   * Django message level integer.
   * 20 = info, 25 = success, 30 = warning, 40 = error.
   */
  @Column(nullable = false)
  val level: Int,

  @Column(nullable = false)
  val start: LocalDateTime,

  /** Null means no scheduled end. */
  @Column(name = "\"end\"")
  val end: LocalDateTime? = null,

  @Column(nullable = false, length = 200)
  val headline: String,

  @Column(nullable = false, columnDefinition = "text")
  val message: String = "",
)
