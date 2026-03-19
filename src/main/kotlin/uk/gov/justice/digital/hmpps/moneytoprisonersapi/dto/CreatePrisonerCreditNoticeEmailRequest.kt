package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email

@Schema(description = "Request to create a prisoner credit notice email configuration")
data class CreatePrisonerCreditNoticeEmailRequest(
  @JsonProperty("prison")
  @Schema(description = "Prison NOMIS ID", example = "LEI")
  val prison: String,

  @JsonProperty("email")
  @Schema(description = "Email address for credit notices", example = "clerk@prison.gov.uk")
  @field:Email(message = "Invalid email address")
  val email: String,
)
