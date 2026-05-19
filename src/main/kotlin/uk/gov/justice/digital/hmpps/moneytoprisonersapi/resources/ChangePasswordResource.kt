package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.DjangoOAuth2Authentication
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_CHANGE_PASSWORD
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ChangePassword
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ChangePasswordByTokenRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.AuthenticatedPasswordChangeResult
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
  private val allowedClientIds = setOf("cashbook", "bank-admin", "noms-ops")

  private fun fieldRequired(field: String): ResponseEntity<Any> = ResponseEntity.badRequest().body(
    mapOf("errors" to mapOf(field to listOf("This field is required."))),
  )

  private fun incorrectPassword(): ResponseEntity<Any> = ResponseEntity.badRequest().body(
    mapOf("errors" to mapOf("old_password" to listOf("You’ve entered an incorrect password"))),
  )

  // -------------------------------------------------------------------------
  // AUTH-045: POST /change_password/ (authenticated old/new password)
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Change password for authenticated user",
    description = "Changes the authenticated user's password using old_password and new_password. " +
      "The token-only anonymous flow lives at POST /change_password/{code}/.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Password changed successfully"),
      ApiResponse(responseCode = "400", description = "Invalid current password or request body", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "403", description = "Unsupported client application"),
      ApiResponse(responseCode = "401", description = "Authentication required", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "oauth2_provider")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/change_password/")
  fun changePassword(
    @RequestBody request: ChangePassword,
    authentication: Authentication,
  ): ResponseEntity<Any> {
    val clientId = (authentication as? DjangoOAuth2Authentication)?.clientId
      ?: return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
    if (clientId !in allowedClientIds) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
    }
    if (request.oldPassword.isBlank()) {
      return fieldRequired("old_password")
    }
    if (request.newPassword.isBlank()) {
      return fieldRequired("new_password")
    }
    return when (
      passwordService.changePassword(
        username = authentication.name,
        oldPassword = request.oldPassword,
        newPassword = request.newPassword,
        application = clientId,
      )
    ) {
      is AuthenticatedPasswordChangeResult.Success -> ResponseEntity.noContent().build()
      AuthenticatedPasswordChangeResult.IncorrectPassword -> incorrectPassword()
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
    @RequestBody request: ChangePasswordByTokenRequest,
  ): ResponseEntity<Any> {
    val newPassword = request.newPassword
      ?: return fieldRequired("new_password")
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
