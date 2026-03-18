package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response from the credit prisoners action containing IDs of credits that could not be processed")
data class CreditActionResponse(
  @Schema(description = "IDs of credits that were not in credit_pending state and could not be credited")
  @JsonProperty("conflict_ids")
  val conflictIds: List<Long>,
)
