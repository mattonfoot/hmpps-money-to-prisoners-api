package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Optional payload nested in a ResetPassword request describing how to construct
 * the password-change URL the user receives by email.
 * Mirrors Python's `CreateNewPassword` schema.
 */
@Schema(name = "CreateNewPassword", description = "Password reset URL configuration")
data class CreateNewPassword(
  @JsonProperty("password_change_url")
  @Schema(description = "Template URL for the password change page", required = true)
  val passwordChangeUrl: String,
  @JsonProperty("reset_code_param")
  @Schema(description = "Query parameter name to inject the reset code into", required = true)
  val resetCodeParam: String,
)
