package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Request body for rejecting a security check")
data class RejectCheckRequest(
  @Schema(description = "Reason for the reject decision", example = "Suspicious activity detected")
  @JsonProperty("decision_reason")
  @field:NotBlank(message = "decision_reason is required")
  val decisionReason: String,

  @Schema(description = "One or more rejection reason codes", example = "[\"FIUMONP\"]")
  @JsonProperty("rejection_reasons")
  @field:NotEmpty(message = "rejection_reasons must not be empty")
  val rejectionReasons: List<String>,
)
