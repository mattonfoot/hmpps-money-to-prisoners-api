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
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "disbursement_comments")
class DisbursementComment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "disbursement_comment_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false, columnDefinition = "text")
  val comment: String,

  @Column(length = 100)
  val category: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "disbursement_id")
  var disbursement: Disbursement? = null,

  @Column(name = "user_id")
  var userId: String? = null,

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
