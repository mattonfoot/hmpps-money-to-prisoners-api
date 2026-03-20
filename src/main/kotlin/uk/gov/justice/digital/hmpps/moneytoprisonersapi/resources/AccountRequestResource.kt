package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.AccountRequestDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateAccountRequestRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.AccountRequestService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping(produces = ["application/json"])
@Tag(name = "Account Requests", description = "Self-service account request workflow (AUTH-060 to AUTH-067)")
class AccountRequestResource(
  private val accountRequestService: AccountRequestService,
) {

  // -------------------------------------------------------------------------
  // AUTH-061, AUTH-067: GET /requests/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List pending account requests",
    description = "Returns all pending account requests. " +
      "Use ordering=-created for newest-first (AUTH-067).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of pending requests",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountRequestPaginatedResponse::class))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/requests/")
  fun listRequests(
    @Parameter(description = "Ordering: omit for oldest-first, use -created for newest-first")
    @RequestParam(required = false)
    ordering: String?,
  ): ResponseEntity<PaginatedResponse<AccountRequestDto>> {
    val requests = accountRequestService.listPendingRequests(ordering)
      .map { AccountRequestDto.from(it) }
    return ResponseEntity.ok(PaginatedResponse(count = requests.size, results = requests))
  }

  // -------------------------------------------------------------------------
  // AUTH-060, AUTH-062: POST /requests/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Create an account request",
    description = "Creates a new pending account request. " +
      "No authentication required. " +
      "If a user already exists with the requested username, their details are returned in existingUser (AUTH-062).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Request created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountRequestDto::class))],
      ),
      ApiResponse(responseCode = "400", description = "Missing required field", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PostMapping("/requests/")
  fun createRequest(
    @RequestBody request: CreateAccountRequestRequest,
  ): ResponseEntity<Any> {
    if (request.username.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("username" to listOf("This field is required")))
    }
    val (saved, existingUser) = accountRequestService.createRequest(
      username = request.username,
      firstName = request.firstName ?: "",
      lastName = request.lastName ?: "",
      email = request.email ?: "",
      roleName = request.role,
      prisonId = request.prison,
    )
    return ResponseEntity.status(HttpStatus.CREATED).body(AccountRequestDto.from(saved, existingUser))
  }

  // -------------------------------------------------------------------------
  // AUTH-063: PATCH /requests/{id}/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Accept an account request",
    description = "Accepts the pending request, creating or updating the MTP user account (AUTH-063).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Request accepted",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountRequestDto::class))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "Request not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt")
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/requests/{id}/")
  fun acceptRequest(
    @Parameter(description = "Account request ID") @PathVariable id: Long,
  ): ResponseEntity<Any> {
    val accepted = accountRequestService.acceptRequest(id) ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(AccountRequestDto.from(accepted))
  }

  // -------------------------------------------------------------------------
  // AUTH-066: DELETE /requests/{id}/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Reject an account request",
    description = "Rejects the pending request by setting its status to rejected (AUTH-066).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Request rejected"),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "Request not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt")
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/requests/{id}/")
  fun rejectRequest(
    @Parameter(description = "Account request ID") @PathVariable id: Long,
  ): ResponseEntity<Any> {
    accountRequestService.rejectRequest(id) ?: return ResponseEntity.notFound().build()
    return ResponseEntity.noContent().build()
  }
}

@Schema(name = "PaginatedResponseAccountRequestDto", description = "Paginated response containing account requests")
private class AccountRequestPaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of account requests")
  val results: List<AccountRequestDto>,
)
