package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequestStatus
import java.time.OffsetDateTime

@Schema(description = "Account request details")
data class AccountRequest(
  @Schema(description = "Request ID")
  val id: Long?,

  @Schema(description = "Requested username")
  val username: String,

  @Schema(description = "First name")
  val firstName: String,

  @Schema(description = "Last name")
  val lastName: String,

  @Schema(description = "Email address")
  val email: String,

  @Schema(description = "Reason for requesting access")
  val reason: String,

  @Schema(description = "Manager email address for security requests")
  @JsonProperty("manager_email")
  val managerEmail: String?,

  @Schema(description = "Requested role name, or null if none")
  val role: String?,

  @Schema(description = "Requested prison NOMIS ID, or null if none")
  val prison: String?,

  @Schema(description = "Request status: pending, accepted, or rejected")
  val status: String,

  @Schema(description = "Timestamp when the request was created")
  val created: OffsetDateTime?,

  @Schema(description = "Timestamp when the request was last modified")
  val modified: OffsetDateTime?,

  @Schema(description = "Existing user with this username, if one already exists (AUTH-062)")
  val existingUser: UserDto? = null,
) {
  companion object {
    fun from(request: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest, existingUser: UserDto? = null) = AccountRequest(
      id = request.id,
      username = request.username,
      firstName = request.firstName,
      lastName = request.lastName,
      email = request.email,
      reason = request.reason,
      managerEmail = request.managerEmail,
      role = request.role?.name,
      prison = request.prison?.nomisId,
      // Django has no `status` column on mtp_auth_accountrequest — presence-of-row
      // means pending. Surface "pending" while the request exists.
      status = AccountRequestStatus.PENDING.name,
      created = request.created,
      modified = request.modified,
      existingUser = existingUser,
    )
  }
}

@Schema(hidden = true)
data class CreateAccountRequestRequest(
  @Schema(description = "Requested username")
  val username: String?,

  @Schema(description = "First name")
  @com.fasterxml.jackson.annotation.JsonProperty("first_name")
  val firstName: String?,

  @Schema(description = "Last name")
  @com.fasterxml.jackson.annotation.JsonProperty("last_name")
  val lastName: String?,

  @Schema(description = "Email address")
  val email: String?,

  @Schema(description = "Reason for requesting access")
  val reason: String? = null,

  @Schema(description = "Role name being requested")
  val role: String?,

  @Schema(description = "Prison NOMIS ID being requested")
  val prison: String?,

  @Schema(description = "Manager email address for security requests")
  @com.fasterxml.jackson.annotation.JsonProperty("manager_email")
  val managerEmail: String? = null,

  @Schema(description = "Set to 'true' to allow a request for a username that already has a role (changes their role).")
  @com.fasterxml.jackson.annotation.JsonProperty("change-role")
  val changeRole: String? = null,
)
