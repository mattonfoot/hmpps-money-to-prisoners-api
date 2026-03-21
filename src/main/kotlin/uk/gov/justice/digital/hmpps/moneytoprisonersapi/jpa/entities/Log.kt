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
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class LogAction {
  CREATED,
  CREDITED,
  REFUNDED,
  RECONCILED,
  REVIEWED,
  MANUAL,
  FAILED,
  EDITED,
  REJECTED,
  CONFIRMED,
  SENT,
  PRECONFIRMED,
}

@Entity
@Table(name = "credit_log")
class Log(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "log_id", columnDefinition = "serial")
  val id: Long? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  val action: LogAction,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_id")
  var credit: Credit? = null,

  @Column(name = "user_id")
  var userId: String? = null,

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,
) {

  @PrePersist
  fun onCreate() {
    if (created == null) {
      created = LocalDateTime.now()
    }
  }
}
