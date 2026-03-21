package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "credit_processingbatch")
class Batch(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "batch_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false)
  var owner: String,

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "credit_processingbatch_credits",
    joinColumns = [JoinColumn(name = "batch_id")],
    inverseJoinColumns = [JoinColumn(name = "credit_id")],
  )
  var credits: MutableList<Credit> = mutableListOf(),

  @Column(nullable = false, updatable = false)
  var created: LocalDateTime? = null,
) {

  @PrePersist
  fun onCreate() {
    created = LocalDateTime.now()
  }
}
