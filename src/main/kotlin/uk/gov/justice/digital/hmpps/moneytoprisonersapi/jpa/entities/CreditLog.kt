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
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "credit_log",
  schema = "public",
  indexes = [
    Index(
      name = "credit_log_created_6416a6_idx",
      columnList = "created",
    ),
    Index(
      name = "credit_log_credit_id_146e30e3",
      columnList = "credit_id",
    ),
    Index(
      name = "credit_log_user_id_c4e9ae4e",
      columnList = "user_id",
    ),
  ],
)
open class CreditLog {
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

  @Size(max = 50)
  @NotNull
  @Column(name = "action", nullable = false, length = 50)
  open var action: String = ""

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "credit_id", nullable = false)
  open var credit: CreditCredit? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  open var user: AuthUser? = null
}
