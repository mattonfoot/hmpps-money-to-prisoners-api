package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for bulk disbursement actions")
data class DisbursementActionRequest(
  @Schema(description = "List of disbursement IDs to act on", required = true)
  @JsonProperty("disbursement_ids")
  val disbursementIds: List<Long>,
)
