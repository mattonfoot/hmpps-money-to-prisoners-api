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
@Table(name = "comments")
class Comment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "comment_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false, columnDefinition = "text")
  val comment: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_id")
  var credit: Credit? = null,

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
