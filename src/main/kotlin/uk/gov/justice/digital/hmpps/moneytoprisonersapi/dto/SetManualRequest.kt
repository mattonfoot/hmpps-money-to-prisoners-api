package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for the set-manual action, containing credit IDs to transition to manual resolution")
data class SetManualRequest(
  @Schema(description = "IDs of credits to set to manual resolution", example = "[1, 2, 3]")
  @JsonProperty("credit_ids")
  val creditIds: List<Long>,
)
