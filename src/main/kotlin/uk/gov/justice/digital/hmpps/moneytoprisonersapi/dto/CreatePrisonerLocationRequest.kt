package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Request to create a prisoner location")
data class CreatePrisonerLocationRequest(
  @JsonProperty("prisoner_number")
  @Schema(description = "Prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @JsonProperty("prison")
  @Schema(description = "Prison NOMIS ID", example = "LEI")
  val prison: String,

  @JsonProperty("active")
  @Schema(description = "Whether location is active (defaults to true)", example = "true")
  val active: Boolean? = true,

  @JsonProperty("prisoner_dob")
  @Schema(description = "Prisoner date of birth", example = "1990-01-01")
  val prisonerDob: LocalDate? = null,
)
