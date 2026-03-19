package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to attach profiles to a list of credits")
data class AttachProfilesRequest(
  @Schema(description = "List of credit IDs to attach profiles to", example = "[1, 2, 3]")
  @JsonProperty("credit_ids")
  val creditIds: List<Long>,
)
