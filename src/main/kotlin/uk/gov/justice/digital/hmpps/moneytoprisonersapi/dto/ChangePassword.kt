package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Authenticated password change request body.
 * Mirrors Python's `ChangePassword` schema.
 */
@Schema(name = "ChangePassword", description = "Authenticated password change request")
data class ChangePassword(
  @JsonProperty("old_password")
  @Schema(description = "Current password", required = true)
  val oldPassword: String,
  @JsonProperty("new_password")
  @Schema(description = "New password", required = true)
  val newPassword: String,
)
