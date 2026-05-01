package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Short prison representation `{nomis_id, name}` used in nested references on
 * profile and check schemas. Mirrors Python's `NOMIS Prison` schema.
 */
@Schema(name = "NOMIS Prison", description = "Short prison representation")
data class NomisPrison(
  @JsonProperty("nomis_id")
  val nomisId: String,
  val name: String,
)
