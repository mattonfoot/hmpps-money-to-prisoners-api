package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole

@Schema(description = "MTP role definition")
data class MtpRoleDto(
  @Schema(description = "Role name (unique identifier)")
  val name: String,

  @Schema(description = "Primary group associated with this role")
  val keyGroup: String,

  @Schema(description = "Additional groups associated with this role (comma-separated)")
  val otherGroups: String,

  @Schema(description = "Application this role belongs to (e.g. cashbook, noms-ops)")
  val application: String,
) {
  companion object {
    fun from(role: MtpRole): MtpRoleDto = MtpRoleDto(
      name = role.name,
      keyGroup = role.keyGroup,
      otherGroups = role.otherGroups,
      application = role.application,
    )
  }
}
