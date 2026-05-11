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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
  name = "credit_privateestatebatch",
  schema = "public",
  indexes = [
    Index(
      name = "credit_privateestatebatch_prison_id_42e504a4",
      columnList = "prison_id",
    ),
    Index(
      name = "credit_privateestatebatch_prison_id_42e504a4_like",
      columnList = "prison_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "credit_privateestatebatch_date_prison_id_e5509171_uniq",
      columnNames = [
        "date",
        "prison_id",
      ],
    ),
  ],
)
open class CreditPrivateestatebatch {
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

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null

  @OneToMany(mappedBy = "privateEstateBatch", fetch = FetchType.EAGER)
  open var credits: MutableList<CreditCredit> = mutableListOf()
}
