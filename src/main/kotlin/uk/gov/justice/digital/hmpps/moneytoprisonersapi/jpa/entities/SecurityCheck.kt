package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class CheckStatus {
  PENDING,
  ACCEPTED,
  REJECTED,
}

@Entity
@Table(name = "security_check")
class SecurityCheck(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "check_id", columnDefinition = "serial")
  val id: Long? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var status: CheckStatus = CheckStatus.PENDING,

  @Column(columnDefinition = "text")
  var description: String? = null,

  @Column(name = "decision_reason", columnDefinition = "text")
  var decisionReason: String? = null,

  @Column(name = "actioned_by")
  var actionedBy: String? = null,

  @Column(name = "actioned_at")
  var actionedAt: LocalDateTime? = null,

  /** JSON array of rule codes that triggered this check, e.g. ["FIUMONP","CSFREQ"] */
  @Column(name = "rule_codes", columnDefinition = "text")
  var ruleCodes: String? = null,

  /** JSON array of human-readable descriptions matching the rule codes */
  @Column(columnDefinition = "text")
  var descriptions: String? = null,

  /** JSON array of rejection reason codes, populated on reject */
  @Column(name = "rejection_reasons", columnDefinition = "text")
  var rejectionReasons: String? = null,

  @Column(name = "started_at")
  var startedAt: LocalDateTime? = null,

  @Column(name = "assigned_to")
  var assignedTo: String? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_id", nullable = false, unique = true)
  var credit: Credit? = null,

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
}
