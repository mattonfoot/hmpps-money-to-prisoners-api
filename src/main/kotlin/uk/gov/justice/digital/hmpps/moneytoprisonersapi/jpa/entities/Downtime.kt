package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Records a period of downtime for a named service.
 *
 * SVC-001 to SVC-005: queried by GET /service-availability/ to compute per-service status.
 * SVC-005: A null [end] means the downtime is ongoing.
 */
@Entity
@Table(name = "service_downtime")
class Downtime(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", columnDefinition = "serial")
  val id: Long? = null,

  /** Identifier matching a known service name (e.g. "gov_uk_pay"). */
  @Column(nullable = false, length = 50)
  val service: String,

  @Column(name = "start_time", nullable = false)
  val start: LocalDateTime,

  /** Null when the downtime has no scheduled end (ongoing). */
  @Column(name = "end_time")
  val end: LocalDateTime? = null,

  /** Optional human-readable message shown to users during downtime. */
  @Column(name = "message_to_users", nullable = false, length = 255)
  val messageToUsers: String = "",
)
