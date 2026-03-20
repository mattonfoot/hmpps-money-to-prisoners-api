package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser

@Schema(description = "MTP user details")
data class UserDto(
  @Schema(description = "User ID")
  val id: Long?,

  @Schema(description = "Username")
  val username: String,

  @Schema(description = "Email address")
  val email: String,

  @Schema(description = "First name")
  val firstName: String,

  @Schema(description = "Last name")
  val lastName: String,

  @Schema(description = "Whether the user account is active")
  val isActive: Boolean,

  @Schema(description = "Assigned role name, or null if none")
  val roleName: String?,

  @Schema(description = "Application the role belongs to, or null if no role")
  val roleApplication: String?,

  @Schema(description = "NOMIS IDs of prisons assigned to this user")
  val prisonIds: List<String>,

  @Schema(description = "Whether the account is locked due to too many failed logins")
  val isLocked: Boolean,
) {
  companion object {
    fun from(user: MtpUser, isLocked: Boolean): UserDto = UserDto(
      id = user.id,
      username = user.username,
      email = user.email,
      firstName = user.firstName,
      lastName = user.lastName,
      isActive = user.isActive,
      roleName = user.role?.name,
      roleApplication = user.role?.application,
      prisonIds = user.prisons.map { it.nomisId }.sorted(),
      isLocked = isLocked,
    )
  }
}
