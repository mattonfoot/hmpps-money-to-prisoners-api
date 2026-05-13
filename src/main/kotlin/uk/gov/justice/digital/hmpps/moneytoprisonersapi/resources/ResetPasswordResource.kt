package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_RESET_PASSWORD
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ResetPasswordRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordResetResult
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping(produces = ["application/json"])
@Tag(name = TAG_RESET_PASSWORD)
class ResetPasswordResource(
  private val passwordService: PasswordService,
) {

  // -------------------------------------------------------------------------
  // AUTH-043 / AUTH-044: POST /reset_password/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Initiate a password reset",
    description = "Initiates a password reset by username or email. " +
      "Returns 204 if reset initiated; 400 for any failure (user not found, locked, no email, ambiguous — AUTH-043 to AUTH-049). " +
      "No authentication required.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Reset initiated — token created (email would be sent in production)"),
      ApiResponse(responseCode = "400", description = "User not found, account locked, no email, or multiple users", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirements
  @PreAuthorize("permitAll()")
  @PostMapping("/reset_password/")
  fun resetPassword(
    @RequestBody request: ResetPasswordRequest,
  ): ResponseEntity<Any> {
    if (request.username.isNullOrBlank() && request.email.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("errors" to mapOf("username" to listOf("This field is required"))))
    }
    val application = request.application ?: ""
    // Mirrors Python's ResetPasswordView: all failure modes return 400 with an
    // `errors.<field>: [<message>]` body keyed by the `username` form field.
    return when (passwordService.initiatePasswordReset(request.username, request.email, application)) {
      is PasswordResetResult.TokenCreated -> ResponseEntity.noContent().build()
      is PasswordResetResult.UserNotFound -> badRequest("Username doesn't match any user account")
      is PasswordResetResult.AccountLocked -> badRequest("Your account is locked, please contact the person who set it up")
      is PasswordResetResult.NoEmail -> badRequest("We don't have your email address, please contact the person who set up the account")
      is PasswordResetResult.MultipleUsers -> badRequest("That email address matches multiple user accounts, please enter your unique username")
    }
  }

  private fun badRequest(message: String): ResponseEntity<Any> = ResponseEntity
    .badRequest()
    .body(mapOf("errors" to mapOf("username" to listOf(message))))
}
