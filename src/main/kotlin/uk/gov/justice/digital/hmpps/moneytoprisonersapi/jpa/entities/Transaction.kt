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

@Entity
@Table(name = "transactions")
class Transaction(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "transaction_id", columnDefinition = "serial")
  val id: Long? = null,

  @Column(nullable = false)
  val amount: Long = 0,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var category: TransactionCategory = TransactionCategory.CREDIT,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var source: TransactionSource = TransactionSource.BANK_TRANSFER,

  @Column(name = "sender_sort_code", length = 50)
  var senderSortCode: String? = null,

  @Column(name = "sender_account_number", length = 50)
  var senderAccountNumber: String? = null,

  @Column(name = "sender_name", length = 250)
  var senderName: String? = null,

  @Column(name = "sender_roll_number", length = 50)
  var senderRollNumber: String? = null,

  @Column(columnDefinition = "text")
  var reference: String? = null,

  @Column(name = "received_at")
  var receivedAt: LocalDateTime? = null,

  @Column(name = "ref_code", length = 50)
  var refCode: String? = null,

  @Column(name = "incomplete_sender_info", nullable = false)
  var incompleteSenderInfo: Boolean = false,

  @Column(name = "reference_in_sender_field", nullable = false)
  var referenceInSenderField: Boolean = false,

  @Column(name = "processor_type_code", length = 50)
  var processorTypeCode: String? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_id", nullable = true, unique = true)
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
