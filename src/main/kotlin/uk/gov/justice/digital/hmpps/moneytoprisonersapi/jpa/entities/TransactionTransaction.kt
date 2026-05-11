package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "transaction_transaction",
  schema = "public",
  indexes = [
    Index(
      name = "transaction_transaction_received_at_3d6eae1a",
      columnList = "received_at",
    ),
    Index(
      name = "transaction_transaction_category_aa75c3cf",
      columnList = "category",
    ),
    Index(
      name = "transaction_transaction_category_aa75c3cf_like",
      columnList = "category",
    ),
    Index(
      name = "transaction_transaction_source_5bc5b6e3",
      columnList = "source",
    ),
    Index(
      name = "transaction_transaction_source_5bc5b6e3_like",
      columnList = "source",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "transaction_transaction_credit_id_key",
      columnNames = ["credit_id"],
    ),
  ],
)
open class TransactionTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "modified", nullable = false)
  open var modified: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "amount", nullable = false)
  open var amount: Long = 0L

  @NotNull
  @Column(name = "reference", nullable = false, length = Integer.MAX_VALUE)
  open var reference: String = ""

  @NotNull
  @Column(name = "received_at", nullable = false)
  open var receivedAt: OffsetDateTime = OffsetDateTime.now()

  @Size(max = 50)
  @NotNull
  @Column(name = "sender_account_number", nullable = false, length = 50)
  open var senderAccountNumber: String = ""

  @Size(max = 250)
  @NotNull
  @Column(name = "sender_name", nullable = false, length = 250)
  open var senderName: String = ""

  @Size(max = 50)
  @NotNull
  @Column(name = "sender_roll_number", nullable = false, length = 50)
  open var senderRollNumber: String = ""

  @Size(max = 50)
  @NotNull
  @Column(name = "sender_sort_code", nullable = false, length = 50)
  open var senderSortCode: String = ""

  @Size(max = 50)
  @NotNull
  @Column(name = "category", nullable = false, length = 50)
  open var category: String = ""

  @Size(max = 12)
  @Column(name = "ref_code", length = 12)
  open var refCode: String? = null

  @Size(max = 50)
  @NotNull
  @Column(name = "source", nullable = false, length = 50)
  open var source: String = ""

  @NotNull
  @Column(name = "incomplete_sender_info", nullable = false)
  open var incompleteSenderInfo: Boolean = false

  @Size(max = 12)
  @Column(name = "processor_type_code", length = 12)
  open var processorTypeCode: String? = null

  @NotNull
  @Column(name = "reference_in_sender_field", nullable = false)
  open var referenceInSenderField: Boolean = false

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "credit_id")
  open var credit: CreditCredit? = null
}
