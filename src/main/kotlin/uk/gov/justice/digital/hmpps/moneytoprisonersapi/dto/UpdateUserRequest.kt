package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for partial update of an MTP user")
data class UpdateUserRequest(
  @Schema(description = "New email address")
  val email: String? = null,

  @Schema(description = "New first name")
  val firstName: String? = null,

  @Schema(description = "New last name")
  val lastName: String? = null,

  @Schema(description = "NOMIS IDs of prisons to assign (replaces existing set)")
  val prisonIds: List<String>? = null,

  @Schema(description = "Role name to assign (admin only; cannot change own role)")
  val roleName: String? = null,
)
