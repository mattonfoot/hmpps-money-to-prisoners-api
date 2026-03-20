package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Records user actions in the system (UEL-001 to UEL-006).
 *
 * Captures the authenticated user, the request path, and optional structured JSON data.
 * Default ordering: timestamp DESC, id DESC (most recent first).
 */
@Entity
@Table(name = "user_events")
class UserEvent(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  var user: MtpUser? = null,

  @Column(name = "path")
  var path: String? = null,

  @Column(name = "data", columnDefinition = "text")
  var data: String? = null,

  @Column(name = "timestamp", nullable = false, updatable = false)
  var timestamp: LocalDateTime? = null,
) {
  @PrePersist
  fun onCreate() {
    if (timestamp == null) {
      timestamp = LocalDateTime.now()
    }
  }
}
