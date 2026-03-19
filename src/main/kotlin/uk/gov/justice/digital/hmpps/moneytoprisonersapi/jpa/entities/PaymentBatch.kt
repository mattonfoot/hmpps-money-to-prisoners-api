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
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "payment_batches")
class PaymentBatch(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_batch_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(name = "ref_code", nullable = false)
  var refCode: Int = 1,

  @Column(name = "settlement_date")
  var settlementDate: LocalDate? = null,

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "payment_batch_credits",
    joinColumns = [JoinColumn(name = "payment_batch_id")],
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
