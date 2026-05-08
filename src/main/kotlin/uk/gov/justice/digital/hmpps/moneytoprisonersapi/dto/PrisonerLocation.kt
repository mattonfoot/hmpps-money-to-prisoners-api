package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime

@Schema(description = "A prisoner location record")
data class PrisonerLocation(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,

  @Schema(description = "Prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "Prison NOMIS ID", example = "LEI")
  val prison: String,

  @Schema(description = "Whether this location is active", example = "true")
  val active: Boolean,

  @Schema(description = "Username of the user who created this record", example = "clerk1")
  val createdBy: String,

  @Schema(description = "Prisoner date of birth", example = "1990-01-01")
  val prisonerDob: LocalDate?,

  @Schema(description = "Timestamp when the record was created")
  val created: OffsetDateTime?,

  @Schema(description = "Timestamp when the record was last modified")
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(loc: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerLocation): PrisonerLocation = PrisonerLocation(
      id = loc.id,
      prisonerNumber = loc.prisonerNumber,
      prison = loc.prison?.nomisId ?: "",
      active = loc.active,
      createdBy = loc.createdBy?.username ?: "",
      prisonerDob = loc.prisonerDob,
      created = loc.created,
      modified = loc.modified,
    )
  }
}
