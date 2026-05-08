package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserFlag

@Schema(name = "Flag", description = "A flag set on an MTP user account")
data class UserFlagDto(
  @Schema(description = "Flag identifier")
  val id: Long?,

  @Schema(description = "Flag name")
  val flagName: String,
) {
  companion object {
    fun from(flag: UserFlag): UserFlagDto = UserFlagDto(id = flag.id, flagName = flag.name)
  }
}

@Schema(hidden = true)
data class CreateUserFlagRequest(
  @Schema(description = "Flag name to set", required = true)
  val flagName: String?,
)
