package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull

@Entity
@Table(
  name = "credit_processingbatch_credits",
  schema = "public",
  indexes = [
    Index(
      name = "credit_processingbatch_credits_processingbatch_id_0077dbd8",
      columnList = "processingbatch_id",
    ),
    Index(
      name = "credit_processingbatch_credits_credit_id_18e4d806",
      columnList = "credit_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "credit_processingbatch_c_processingbatch_id_credi_8da4c226_uniq",
      columnNames = [
        "processingbatch_id",
        "credit_id",
      ],
    ),
  ],
)
open class CreditProcessingbatchCredit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "processingbatch_id", nullable = false)
  open var processingbatch: CreditProcessingbatch? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credit_id", nullable = false)
  open var credit: CreditCredit? = null
}
