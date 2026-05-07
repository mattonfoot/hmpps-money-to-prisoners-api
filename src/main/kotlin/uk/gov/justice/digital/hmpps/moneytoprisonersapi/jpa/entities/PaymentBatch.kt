package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
  name = "payment_batch",
  schema = "public",
  uniqueConstraints = [
    UniqueConstraint(
      name = "payment_batch_settlement_transaction_id_key",
      columnNames = ["settlement_transaction_id"],
    ),
  ],
)
open class PaymentBatch {
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
  @Column(name = "date", nullable = false)
  open var date: LocalDate = LocalDate.now()

  @Size(max = 12)
  @NotNull
  @Column(name = "ref_code", nullable = false, length = 12)
  open var refCode: String = ""

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "settlement_transaction_id")
  open var settlementTransaction: TransactionTransaction? =
    null
}
