package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
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
  name = "prison_prisonerbalance",
  schema = "public",
  indexes = [
    Index(
      name = "prison_pris_prisone_e452a0_idx",
      columnList = "prisoner_number, prison_id",
    ),
    Index(
      name = "prison_prisonerbalance_prison_id_234fcc91",
      columnList = "prison_id",
    ),
    Index(
      name = "prison_prisonerbalance_prison_id_234fcc91_like",
      columnList = "prison_id",
    ),
  ],
)
open class PrisonPrisonerbalance {
  @Id
  @Size(max = 250)
  @Column(name = "prisoner_number", nullable = false, length = 250)
  open var prisonerNumber: String = ""

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
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null
}
