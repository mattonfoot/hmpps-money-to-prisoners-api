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
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
  name = "prison_prisonerlocation",
  schema = "public",
  indexes = [
    Index(
      name = "prison_pris_prisone_6c3f58_idx",
      columnList = "prisoner_number, prisoner_dob",
    ),
    Index(
      name = "prison_prisonerlocation_created_by_id_9f6dccb7",
      columnList = "created_by_id",
    ),
    Index(
      name = "prison_prisonerlocation_prison_id_7be9033c",
      columnList = "prison_id",
    ),
    Index(
      name = "prison_prisonerlocation_prison_id_7be9033c_like",
      columnList = "prison_id",
    ),
    Index(
      name = "prison_prisonerlocation_active_270bdb65",
      columnList = "active",
    ),
  ],
)
open class PrisonPrisonerlocation {
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

  @Size(max = 250)
  @NotNull
  @Column(name = "prisoner_number", nullable = false, length = 250)
  open var prisonerNumber: String = ""

  @NotNull
  @Column(name = "prisoner_dob", nullable = false)
  open var prisonerDob: LocalDate = LocalDate.now()

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id")
  open var createdBy: AuthUser? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prison_id", nullable = false)
  open var prison: PrisonPrison? = null

  @Size(max = 250)
  @NotNull
  @Column(name = "prisoner_name", nullable = false, length = 250)
  open var prisonerName: String = ""

  @NotNull
  @Column(name = "active", nullable = false)
  open var active: Boolean = false
}
