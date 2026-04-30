package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_PRISONS
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonCategoryDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonPopulationDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = TAG_PRISONS)
class PrisonResource(
  private val prisonService: PrisonService,
) {

  @Operation(
    summary = "List prisons",
    description = "Returns a paginated list of all prisons. This endpoint is public and requires no authentication. " +
      "Optionally filter to prisons with at least one active prisoner location using exclude_empty_prisons=true.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of prisons",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonPaginatedResponse::class))],
      ),
    ],
  )
  @SecurityRequirement(name = "bearer-jwt")
  @PreAuthorize("permitAll()")
  @GetMapping("/prisons/", produces = ["application/json"])
  fun listPrisons(
    @Parameter(description = "Exclude prisons with no active prisoner locations", example = "false")
    @RequestParam("exclude_empty_prisons")
    excludeEmptyPrisons: Boolean = false,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<PrisonDto> {
    val prisons = prisonService.listPrisons(excludeEmptyPrisons)
    val results = prisons.map { PrisonDto.from(it) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(
    summary = "List prison populations",
    description = "Returns a paginated list of all prison population types. Requires authentication.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of prison populations",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonPopulationPaginatedResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("permitAll()")
  @GetMapping("/prison_populations/", produces = ["application/json"])
  fun listPrisonPopulations(
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<PrisonPopulationDto> {
    val populations = prisonService.listPrisonPopulations()
    val results = populations.map { PrisonPopulationDto.from(it) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(
    summary = "List prison categories",
    description = "Returns a paginated list of all prison category types. Requires authentication.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of prison categories",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonCategoryPaginatedResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("permitAll()")
  @GetMapping("/prison_categories/", produces = ["application/json"])
  fun listPrisonCategories(
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<PrisonCategoryDto> {
    val categories = prisonService.listPrisonCategories()
    val results = categories.map { PrisonCategoryDto.from(it) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }
}

@Schema(name = "PaginatedResponsePrisonDto", description = "Paginated response containing prison records")
private class PrisonPaginatedResponse(
  val count: Int,
  val next: String?,
  val previous: String?,
  val results: List<PrisonDto>,
)

@Schema(name = "PaginatedResponsePrisonPopulationDto", description = "Paginated response containing prison population records")
private class PrisonPopulationPaginatedResponse(
  val count: Int,
  val next: String?,
  val previous: String?,
  val results: List<PrisonPopulationDto>,
)

@Schema(name = "PaginatedResponsePrisonCategoryDto", description = "Paginated response containing prison category records")
private class PrisonCategoryPaginatedResponse(
  val count: Int,
  val next: String?,
  val previous: String?,
  val results: List<PrisonCategoryDto>,
)
