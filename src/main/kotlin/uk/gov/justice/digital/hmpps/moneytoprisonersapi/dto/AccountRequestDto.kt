package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest
import java.time.LocalDateTime

@Schema(description = "Account request details")
data class AccountRequestDto(
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

  @Schema(description = "Requested role name, or null if none")
  val role: String?,

  @Schema(description = "Requested prison NOMIS ID, or null if none")
  val prison: String?,

  @Schema(description = "Request status: pending, accepted, or rejected")
  val status: String,

  @Schema(description = "Timestamp when the request was created")
  val created: LocalDateTime?,

  @Schema(description = "Timestamp when the request was last modified")
  val modified: LocalDateTime?,

  @Schema(description = "Existing user with this username, if one already exists (AUTH-062)")
  val existingUser: UserDto? = null,
) {
  companion object {
    fun from(request: AccountRequest, existingUser: UserDto? = null) = AccountRequestDto(
      id = request.id,
      username = request.username,
      firstName = request.firstName,
      lastName = request.lastName,
      email = request.email,
      role = request.role?.name,
      prison = request.prison?.nomisId,
      status = request.status,
      created = request.created,
      modified = request.modified,
      existingUser = existingUser,
    )
  }
}

@Schema(description = "Request body for creating an account request")
data class CreateAccountRequestRequest(
  @Schema(description = "Requested username")
  val username: String?,

  @Schema(description = "First name")
  val firstName: String?,

  @Schema(description = "Last name")
  val lastName: String?,

  @Schema(description = "Email address")
  val email: String?,

  @Schema(description = "Role name being requested")
  val role: String?,

  @Schema(description = "Prison NOMIS ID being requested")
  val prison: String?,
)
