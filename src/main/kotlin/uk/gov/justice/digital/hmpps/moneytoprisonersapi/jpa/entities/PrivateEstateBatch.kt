package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "credit_privateestatebatch")
class PrivateEstateBatch(
  @Id
  @Column(name = "ref", length = 30)
  val ref: String,

  @Column(name = "prison", length = 10, nullable = false)
  val prison: String,

  @Column(nullable = false)
  val date: LocalDate,

  @Column(name = "total_amount", nullable = false)
  var totalAmount: Long = 0,

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "credit_privateestatebatch_credits",
    joinColumns = [JoinColumn(name = "ref")],
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
