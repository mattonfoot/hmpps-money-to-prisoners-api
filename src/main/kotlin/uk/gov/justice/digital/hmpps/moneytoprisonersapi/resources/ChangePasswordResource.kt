package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_CHANGE_PASSWORD
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ChangePasswordByTokenRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordChangeResult
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PasswordService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(produces = ["application/json"])
@Tag(name = TAG_CHANGE_PASSWORD)
class ChangePasswordResource(
  private val passwordService: PasswordService,
) {

  // -------------------------------------------------------------------------
  // AUTH-045: POST /change_password/ (via reset token)
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Change password using reset token",
    description = "Completes a password reset using the UUID token issued by POST /reset_password/. " +
      "Authentication required to match Python's ChangePasswordView contract (AUTH-045). " +
      "The token-only anonymous flow lives at POST /change_password/{code}/.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Password changed successfully"),
      ApiResponse(responseCode = "400", description = "Invalid, missing, or already-used token", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Authentication required", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "oauth2_provider")
  @PreAuthorize("isAuthenticated()")
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

  /**
   * Python-compatible: POST /change_password/{code}/ with new_password in body.
   * The code is the UUID token from the password reset email.
   */
  @SecurityRequirements
  @PreAuthorize("permitAll()")
  @PostMapping("/change_password/{code}/")
  fun changePasswordByCode(
    @PathVariable code: String,
    @RequestBody request: Map<String, String>,
  ): ResponseEntity<Any> {
    val newPassword = request["new_password"]
      ?: return ResponseEntity.badRequest().body(mapOf("errors" to mapOf("new_password" to listOf("This field is required."))))
    val token = try {
      UUID.fromString(code)
    } catch (_: IllegalArgumentException) {
      return ResponseEntity.status(404).build<Any>()
    }
    return when (passwordService.changePasswordByToken(token, newPassword)) {
      is PasswordChangeResult.Success -> ResponseEntity.noContent().build()
      is PasswordChangeResult.InvalidToken -> ResponseEntity.status(404).build()
    }
  }
}
