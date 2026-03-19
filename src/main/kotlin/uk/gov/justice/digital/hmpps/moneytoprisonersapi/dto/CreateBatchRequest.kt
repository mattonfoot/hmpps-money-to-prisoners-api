package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for creating a processing batch")
data class CreateBatchRequest(
  @Schema(description = "List of credit IDs to include in the batch")
  @JsonProperty("credit_ids")
  val creditIds: List<Long> = emptyList(),
)
