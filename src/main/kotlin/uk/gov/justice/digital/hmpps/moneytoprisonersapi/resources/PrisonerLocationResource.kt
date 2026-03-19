package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CanUploadResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreatePrisonerLocationRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonerLocationDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal

@RestController
@RequestMapping("/prisoner_locations", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Prisoner Locations", description = "Endpoints for managing prisoner locations")
class PrisonerLocationResource(
  private val prisonService: PrisonService,
) {

  @Operation(
    summary = "Create prisoner locations",
    description = "Bulk creates prisoner location records. Requires ROLE_NOMS_OPS or ROLE_CASHBOOK. " +
      "Existing active locations for the same prisoner+prison are deactivated. " +
      "Body must be an array, not a single object.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Locations created successfully"),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid prison code or body format",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_NOMS_OPS or ROLE_CASHBOOK",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('NOMS_OPS') or hasRole('CASHBOOK')")
  @PostMapping("/")
  fun createPrisonerLocations(
    @RequestBody requests: List<CreatePrisonerLocationRequest>,
    principal: Principal,
  ): ResponseEntity<Any> {
    prisonService.createPrisonerLocations(requests, principal.name)
    return ResponseEntity.status(HttpStatus.CREATED).build()
  }

  @Operation(
    summary = "Get prisoner location",
    description = "Returns the active location record for the given prisoner number.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner location found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonerLocationDto::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No active location found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{prisonerNumber}/")
  fun getPrisonerLocation(
    @PathVariable prisonerNumber: String,
  ): PrisonerLocationDto {
    val location = prisonService.getActivePrisonerLocation(prisonerNumber)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No active location found for prisoner $prisonerNumber")
    return PrisonerLocationDto.from(location)
  }

  @Operation(
    summary = "Check if upload is possible",
    description = "Returns whether a new prisoner location upload can be performed. " +
      "Returns false if any locations were deactivated within the last 10 minutes.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Can upload status",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CanUploadResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/can-upload/")
  fun canUpload(): CanUploadResponse = CanUploadResponse(canUpload = prisonService.canUpload())

  @Operation(
    summary = "Deactivate all active prisoner locations",
    description = "Sets active=false on all currently active prisoner location records. Requires ROLE_NOMS_OPS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "All active locations deactivated"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_NOMS_OPS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('NOMS_OPS')")
  @PostMapping("/delete_old/")
  fun deleteOld(): ResponseEntity<Any> {
    prisonService.deleteOldLocations()
    return ResponseEntity.noContent().build()
  }

  @Operation(
    summary = "Delete all inactive prisoner locations",
    description = "Permanently deletes all prisoner location records where active=false. Requires ROLE_NOMS_OPS.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "All inactive locations deleted"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_NOMS_OPS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('NOMS_OPS')")
  @PostMapping("/delete_inactive/")
  fun deleteInactive(): ResponseEntity<Any> {
    prisonService.deleteInactiveLocations()
    return ResponseEntity.noContent().build()
  }
}
