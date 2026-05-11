package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole

@Schema(description = "MTP role definition")
data class Role(
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
    fun from(role: MtpRole): Role = Role(
      name = role.name,
      keyGroup = role.keyGroup?.name ?: "",
      // Django models other-groups via mtp_auth_role_other_groups M2M; surface
      // as a comma-joined list. Unit-test factories can override via the
      // transient otherGroupsCsv field on the entity.
      otherGroups = role.otherGroupsCsv
        ?: role.otherGroups.joinToString(",") { it.name },
      application = role.application?.clientId ?: "",
    )
  }
}
