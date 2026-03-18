package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for the review action, containing credit IDs to mark as reviewed")
data class ReviewRequest(
  @Schema(description = "IDs of credits to mark as reviewed", example = "[1, 2, 3]")
  @JsonProperty("credit_ids")
  val creditIds: List<Long>,
)
