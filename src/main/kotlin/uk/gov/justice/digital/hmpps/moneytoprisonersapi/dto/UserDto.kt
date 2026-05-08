package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser

@Schema(name = "Detailed User", description = "MTP user details")
data class UserDto(
  @Schema(description = "User ID")
  val pk: Long?,

  @Schema(description = "User ID (alias)")
  val id: Long?,

  @Schema(description = "Username")
  val username: String,

  @Schema(description = "Email address")
  val email: String,

  @Schema(description = "First name")
  @JsonProperty("first_name")
  val firstName: String,

  @Schema(description = "Last name")
  @JsonProperty("last_name")
  val lastName: String,

  @Schema(description = "Whether the user account is active")
  @JsonProperty("is_active")
  val isActive: Boolean,

  @Schema(description = "Assigned role name, or null if none")
  @JsonProperty("role")
  val roleName: String?,

  @Schema(description = "Application the role belongs to, or null if no role")
  @JsonProperty("role_application")
  val roleApplication: String?,

  @Schema(description = "NOMIS IDs of prisons assigned to this user")
  val prisons: List<String>,

  @Schema(description = "User flags (e.g. hmpps-employee)")
  val flags: List<String>,

  @Schema(description = "Whether the user has UserAdmin permissions")
  @JsonProperty("user_admin")
  val userAdmin: Boolean,

  @Schema(description = "Whether the account is locked due to too many failed logins")
  @JsonProperty("is_locked")
  val isLocked: Boolean,
) {
  companion object {
    fun from(user: MtpUser, isLocked: Boolean, flags: List<String> = emptyList(), isUserAdmin: Boolean = false): UserDto = UserDto(
      pk = user.id,
      id = user.id,
      username = user.username,
      email = user.email,
      firstName = user.firstName,
      lastName = user.lastName,
      isActive = user.isActive,
      // Django has no `role` FK on auth_user — role is computed from group
      // membership via mtp_auth_role.key_group. Surface null until that helper
      // is wired in.
      roleName = null,
      roleApplication = null,
      prisons = user.prisons.map { it.nomisId }.sorted(),
      flags = flags,
      userAdmin = isUserAdmin,
      isLocked = isLocked,
    )
  }
}
