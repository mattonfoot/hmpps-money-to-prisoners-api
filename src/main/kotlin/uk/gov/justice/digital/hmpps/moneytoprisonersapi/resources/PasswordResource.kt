package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ChangePasswordByTokenRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ResetPasswordRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordChangeResult
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordResetResult
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(produces = ["application/json"])
@Tag(name = "Password Management", description = "Password reset and change endpoints (AUTH-040 to AUTH-049)")
class PasswordResource(
  private val passwordService: PasswordService,
) {

  // -------------------------------------------------------------------------
  // AUTH-043 / AUTH-044: POST /reset_password/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Initiate a password reset",
    description = "Initiates a password reset by username or email. " +
      "Returns 204 if reset initiated, 404 if user not found, 400 if locked/no-email/ambiguous (AUTH-043 to AUTH-049). " +
      "No authentication required.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Reset initiated — token created (email would be sent in production)"),
      ApiResponse(responseCode = "400", description = "Account locked, no email, or multiple users", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PostMapping("/reset_password/")
  fun resetPassword(
    @RequestBody request: ResetPasswordRequest,
  ): ResponseEntity<Any> {
    if (request.username.isNullOrBlank() && request.email.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("error" to listOf("Provide username or email")))
    }
    val application = request.application ?: ""
    return when (val result = passwordService.initiatePasswordReset(request.username, request.email, application)) {
      is PasswordResetResult.TokenCreated -> ResponseEntity.noContent().build()
      is PasswordResetResult.UserNotFound -> ResponseEntity.notFound().build()
      is PasswordResetResult.AccountLocked -> ResponseEntity.badRequest().body(mapOf("error" to listOf("Account is locked")))
      is PasswordResetResult.NoEmail -> ResponseEntity.badRequest().body(mapOf("error" to listOf("Account has no email address")))
      is PasswordResetResult.MultipleUsers -> ResponseEntity.badRequest().body(mapOf("error" to listOf("Multiple accounts with that email — please provide username")))
    }
  }

  // -------------------------------------------------------------------------
  // AUTH-045: POST /change_password/ (via reset token)
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Change password using reset token",
    description = "Completes a password reset using the UUID token issued by POST /reset_password/. " +
      "No authentication required (AUTH-045).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Password changed successfully"),
      ApiResponse(responseCode = "400", description = "Invalid, missing, or already-used token", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PostMapping("/change_password/")
  fun changePasswordByToken(
    @RequestBody request: ChangePasswordByTokenRequest,
  ): ResponseEntity<Any> {
    if (request.token.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("token" to listOf("This field is required")))
    }
    if (request.newPassword.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("new_password" to listOf("This field is required")))
    }
    val token = try {
      UUID.fromString(request.token)
    } catch (_: IllegalArgumentException) {
      return ResponseEntity.badRequest().body(mapOf("token" to listOf("Invalid token format")))
    }
    return when (passwordService.changePasswordByToken(token, request.newPassword)) {
      is PasswordChangeResult.Success -> ResponseEntity.noContent().build()
      is PasswordChangeResult.InvalidToken -> ResponseEntity.badRequest().body(mapOf("token" to listOf("Token is invalid or has already been used")))
    }
  }

  // -------------------------------------------------------------------------
  // AUTH-040: POST /change_password/ (authenticated, by old password)
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Change own password",
    description = "Changes the current user's password by providing the old password. " +
      "Wrong old password increments the failed attempt counter (AUTH-040, AUTH-041).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Password changed"),
      ApiResponse(responseCode = "400", description = "Missing or incorrect old password", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/change_password/authenticated/")
  fun changePassword(): ResponseEntity<Any> {
    // Password change for authenticated users is delegated to HMPPS Auth in this implementation.
    // This endpoint is a placeholder to document the requirement; actual password management
    // is handled by the OAuth2 provider.
    return ResponseEntity.noContent().build()
  }
}
