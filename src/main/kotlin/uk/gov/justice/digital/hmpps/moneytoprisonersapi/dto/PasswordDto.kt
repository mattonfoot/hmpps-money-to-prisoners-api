package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for changing password")
data class ChangePasswordRequest(
  @Schema(description = "Current password", required = true)
  val oldPassword: String?,

  @Schema(description = "New password", required = true)
  val newPassword: String?,

  @Schema(description = "Application identifier", example = "cashbook")
  val application: String? = null,
)

@Schema(description = "Request body for initiating a password reset")
data class ResetPasswordRequest(
  @Schema(description = "Username to reset (provide username OR email)", example = "jsmith")
  val username: String? = null,

  @Schema(description = "Email of the account to reset (provide username OR email)", example = "j.smith@example.com")
  val email: String? = null,

  @Schema(description = "Application identifier", example = "cashbook")
  val application: String? = null,
)

@Schema(description = "Request body for completing a password reset via token")
data class ChangePasswordByTokenRequest(
  @Schema(description = "Password reset token (UUID)", required = true)
  val token: String?,

  @Schema(description = "New password", required = true)
  val newPassword: String?,
)
