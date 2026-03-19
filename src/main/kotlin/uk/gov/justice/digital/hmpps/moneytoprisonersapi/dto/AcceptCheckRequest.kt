package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request body for accepting a security check")
data class AcceptCheckRequest(
  @Schema(description = "Reason for the accept decision", example = "Known sender, verified")
  @JsonProperty("decision_reason")
  @field:NotBlank(message = "decision_reason is required")
  val decisionReason: String,
)
