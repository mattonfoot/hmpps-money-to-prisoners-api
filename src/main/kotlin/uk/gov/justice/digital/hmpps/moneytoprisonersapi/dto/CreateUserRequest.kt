package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for creating a new MTP user")
data class CreateUserRequest(
  @Schema(description = "Username (unique, case-insensitive)", required = true)
  val username: String?,

  @Schema(description = "Email address", required = true)
  val email: String?,

  @Schema(description = "First name")
  val firstName: String? = null,

  @Schema(description = "Last name")
  val lastName: String? = null,

  @Schema(description = "Role name to assign")
  val roleName: String? = null,

  @Schema(description = "NOMIS IDs of prisons to assign to the user")
  val prisonIds: List<String>? = null,
)
