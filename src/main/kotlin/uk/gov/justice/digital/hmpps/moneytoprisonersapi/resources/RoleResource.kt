package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.MtpRoleDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpRoleRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping(produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Roles", description = "MTP role definitions (AUTH-020 to AUTH-023)")
class RoleResource(
  private val mtpRoleRepository: MtpRoleRepository,
) {

  // -------------------------------------------------------------------------
  // AUTH-020: GET /roles/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List all MTP roles",
    description = "Returns all defined roles. Any authenticated user can retrieve the list (AUTH-020). " +
      "Each role has a name, key_group, other_groups and application identifier (AUTH-022).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of roles",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = RolePaginatedResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/roles/")
  fun listRoles(): PaginatedResponse<MtpRoleDto> {
    val roles = mtpRoleRepository.findAll().map { MtpRoleDto.from(it) }
    return PaginatedResponse(count = roles.size, results = roles)
  }
}

@Schema(name = "PaginatedResponseMtpRoleDto", description = "Paginated response containing MTP roles")
private class RolePaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of roles")
  val results: List<MtpRoleDto>,
)
