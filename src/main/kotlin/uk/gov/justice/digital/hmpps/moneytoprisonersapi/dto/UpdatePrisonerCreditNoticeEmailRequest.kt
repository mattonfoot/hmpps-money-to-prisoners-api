package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email

@Schema(description = "Request to update a prisoner credit notice email configuration")
data class UpdatePrisonerCreditNoticeEmailRequest(
  @JsonProperty("email")
  @Schema(description = "New email address for credit notices", example = "newclerk@prison.gov.uk")
  @field:Email(message = "Invalid email address")
  val email: String,
)
