package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(hidden = true)
data class CreateUserRequest(
  @Schema(description = "Username (unique, case-insensitive)", required = true)
  val username: String?,

  @Schema(description = "Email address", required = true)
  val email: String?,

  @Schema(description = "First name")
  @JsonProperty("first_name")
  val firstName: String? = null,

  @Schema(description = "Last name")
  @JsonProperty("last_name")
  val lastName: String? = null,

  @Schema(description = "Role name to assign")
  @JsonProperty("role")
  val roleName: String? = null,

  @Schema(description = "NOMIS IDs of prisons to assign to the user")
  @JsonProperty("prisons")
  val prisonIds: List<String>? = null,
)
