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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_USERS
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateUserFlagRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UserFlagDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserFlag
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.UserFlagRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.UserService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping(produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = TAG_USERS)
class UserFlagResource(
  private val userFlagRepository: UserFlagRepository,
  private val userService: UserService,
) {

  // -------------------------------------------------------------------------
  // AUTH-031: GET /users/{id}/flags/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List flags for a user",
    description = "Returns all flags set on the specified user's account (AUTH-031).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of flags",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = FlagPaginatedResponse::class))],
      ),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/users/{user_username}/flags/")
  fun listFlags(
    @Parameter(description = "User ID") @PathVariable("user_username") username: String,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): ResponseEntity<PaginatedResponse<String>> {
    val user = userService.findByUsername(username) ?: return ResponseEntity.notFound().build()
    val flags = userFlagRepository.findByUser(user).map { it.flagName }
    return ResponseEntity.ok(PaginatedResponse.fromList(flags, limit = limit, offset = offset))
  }

  // -------------------------------------------------------------------------
  // AUTH-030: POST /users/{id}/flags/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Create a flag on a user",
    description = "Creates a new flag on the user account. The (user, flag_name) pair must be unique (AUTH-030).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Flag created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = UserFlagDto::class))],
      ),
      ApiResponse(responseCode = "400", description = "Missing or duplicate flag", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/users/{user_username}/flags/")
  fun createFlag(
    @Parameter(description = "User ID") @PathVariable("user_username") username: String,
    @RequestBody request: CreateUserFlagRequest,
  ): ResponseEntity<Any> {
    val user = userService.findByUsername(username) ?: return ResponseEntity.notFound().build()
    if (request.flagName.isNullOrBlank()) {
      return ResponseEntity.badRequest().body(mapOf("flagName" to listOf("This field is required")))
    }
    if (userFlagRepository.existsByUserAndFlagName(user, request.flagName)) {
      return ResponseEntity.badRequest().body(mapOf("flagName" to listOf("Flag already exists for this user")))
    }
    val saved = userFlagRepository.save(UserFlag(user = user, flagName = request.flagName))
    return ResponseEntity.status(HttpStatus.CREATED).body(saved.flagName)
  }

  // -------------------------------------------------------------------------
  // AUTH-032: DELETE /users/{id}/flags/{name}/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Delete a flag from a user",
    description = "Removes the named flag from the user account (AUTH-032).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Flag deleted"),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "404", description = "User or flag not found", content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PutMapping("/users/{user_username}/flags/{name}/")
  fun putFlag(
    @PathVariable("user_username") username: String,
    @PathVariable("name") flagName: String,
  ): ResponseEntity<Any> {
    val user = userService.findByUsername(username) ?: return ResponseEntity.notFound().build()
    if (userFlagRepository.findByUserAndFlagName(user, flagName) == null) {
      userFlagRepository.save(UserFlag(user = user, flagName = flagName))
    }
    return ResponseEntity.noContent().build()
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/users/{user_username}/flags/{name}/")
  fun deleteFlag(
    @Parameter(description = "User ID") @PathVariable("user_username") username: String,
    @Parameter(description = "Flag name to remove") @PathVariable("name") flagName: String,
  ): ResponseEntity<Any> {
    val user = userService.findByUsername(username) ?: return ResponseEntity.notFound().build()
    val flag = userFlagRepository.findByUserAndFlagName(user, flagName)
      ?: return ResponseEntity.notFound().build()
    userFlagRepository.delete(flag)
    return ResponseEntity.noContent().build()
  }
}

@Schema(name = "PaginatedResponseUserFlagDto", description = "Paginated response containing user flags")
private class FlagPaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of flags")
  val results: List<UserFlagDto>,
)
