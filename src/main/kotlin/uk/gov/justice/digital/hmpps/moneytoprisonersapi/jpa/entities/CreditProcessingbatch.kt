package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

@Entity
@Table(
  name = "credit_processingbatch",
  schema = "public",
  indexes = [
    Index(
      name = "credit_processingbatch_user_id_1c014f29",
      columnList = "user_id",
    ),
  ],
)
open class CreditProcessingbatch {
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
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "credit_processingbatch_credits",
    joinColumns = [JoinColumn(name = "processingbatch_id")],
    inverseJoinColumns = [JoinColumn(name = "credit_id")],
  )
  open var credits: MutableSet<CreditCredit> = mutableSetOf()
}
