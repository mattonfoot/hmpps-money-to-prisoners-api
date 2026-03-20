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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateUserRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateUserRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UserDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.UserService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal

@RestController
@RequestMapping(produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Users", description = "MTP user account management (AUTH-010 to AUTH-018)")
class UserResource(
  private val userService: UserService,
) {

  // -------------------------------------------------------------------------
  // AUTH-010: GET /users/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List MTP users",
    description = "Returns a paginated list of MTP users, optionally filtered by role name or prison (AUTH-010).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of users",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = UserPaginatedResponse::class))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/users/")
  fun listUsers(
    @Parameter(description = "Filter by role name") @RequestParam("role") roleName: String?,
    @Parameter(description = "Filter by prison NOMIS ID") @RequestParam("prison") prisonId: String?,
  ): PaginatedResponse<UserDto> {
    val users = userService.listUsers(roleName, prisonId)
    val results = users.map { (user, locked) -> UserDto.from(user, locked) }
    return PaginatedResponse(count = results.size, results = results)
  }

  // -------------------------------------------------------------------------
  // AUTH-011: GET /users/{id}/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Get user details",
    description = "Returns details for a specific MTP user including permissions, prisons, and lock status (AUTH-011).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "User details",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDto::class))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/users/{id}/")
  fun getUser(
    @Parameter(description = "User ID") @PathVariable id: Long,
  ): ResponseEntity<UserDto> {
    val (user, locked) = userService.getUser(id) ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(UserDto.from(user, locked))
  }

  // -------------------------------------------------------------------------
  // AUTH-012: POST /users/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Create a new MTP user",
    description = "Creates a new MTP user with the given username, email, optional role, and prisons (AUTH-012).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "User created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDto::class))],
      ),
      ApiResponse(responseCode = "400", description = "Validation error", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/users/")
  fun createUser(
    @RequestBody request: CreateUserRequest,
  ): ResponseEntity<Any> {
    if (request.username.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("username" to listOf("This field is required")))
    }
    if (request.email.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("email" to listOf("This field is required")))
    }
    val role = userService.findRoleByName(request.roleName)
    val prisons = userService.findPrisonsByIds(request.prisonIds ?: emptyList())
    return try {
      val user = userService.createUser(
        username = request.username,
        email = request.email,
        firstName = request.firstName,
        lastName = request.lastName,
        role = role,
        prisons = prisons,
      )
      ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(user, false))
    } catch (e: IllegalArgumentException) {
      ResponseEntity.badRequest().body(mapOf("error" to listOf(e.message)))
    }
  }

  // -------------------------------------------------------------------------
  // AUTH-013: PATCH /users/{id}/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Update an MTP user",
    description = "Partially updates a user. Users cannot change their own role or prisons (AUTH-013, AUTH-018).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "User updated",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDto::class))],
      ),
      ApiResponse(responseCode = "400", description = "Validation error", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/users/{id}/")
  fun updateUser(
    @Parameter(description = "User ID") @PathVariable id: Long,
    @RequestBody request: UpdateUserRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    val role = userService.findRoleByName(request.roleName)
    val prisons = request.prisonIds?.let { userService.findPrisonsByIds(it) }
    return try {
      val targetUser = userService.findById(id) ?: return ResponseEntity.notFound().build()
      val isSelf = targetUser.username.equals(principal.name, ignoreCase = true)
      val updated = userService.updateUser(
        id = id,
        email = request.email,
        firstName = request.firstName,
        lastName = request.lastName,
        prisons = prisons,
        role = role,
        isSelf = isSelf,
      ) ?: return ResponseEntity.notFound().build()
      val locked = userService.getUser(updated.id!!)?.second ?: false
      ResponseEntity.ok(UserDto.from(updated, locked))
    } catch (e: IllegalArgumentException) {
      ResponseEntity.badRequest().body(mapOf("error" to listOf(e.message)))
    }
  }

  // -------------------------------------------------------------------------
  // AUTH-014: DELETE /users/{id}/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Deactivate an MTP user",
    description = "Deactivates the user (sets is_active=false). The account is not deleted (AUTH-014).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "User deactivated"),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/users/{id}/")
  fun deleteUser(
    @Parameter(description = "User ID") @PathVariable id: Long,
  ): ResponseEntity<Any> {
    userService.deactivateUser(id) ?: return ResponseEntity.notFound().build()
    return ResponseEntity.noContent().build()
  }

  // -------------------------------------------------------------------------
  // AUTH-017: POST /users/{id}/unlock/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Unlock a user account",
    description = "Clears all failed login attempts for the user, unlocking their account (AUTH-017).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "User unlocked",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDto::class))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/users/{id}/unlock/")
  fun unlockUser(
    @Parameter(description = "User ID") @PathVariable id: Long,
  ): ResponseEntity<UserDto> {
    userService.unlockUser(id) ?: return ResponseEntity.notFound().build()
    val (user, locked) = userService.getUser(id) ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(UserDto.from(user, locked))
  }
}

@Schema(name = "PaginatedResponseUserDto", description = "Paginated response containing MTP users")
private class UserPaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of users")
  val results: List<UserDto>,
)
