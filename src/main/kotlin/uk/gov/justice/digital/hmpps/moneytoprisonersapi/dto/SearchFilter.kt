package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * A single field/value filter pair, used in saved searches.
 *
 * Mirrors Python's `SearchFilter` schema for serialiser-level compatibility.
 */
@Schema(name = "SearchFilter", description = "A single field/value filter pair within a saved search")
data class SearchFilter(
  @Schema(description = "The query parameter name to filter by", example = "prison")
  val field: String,
  @Schema(description = "The filter value", example = "LEI")
  val value: String,
)
