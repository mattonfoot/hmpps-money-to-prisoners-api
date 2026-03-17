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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreditDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/credits", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Credits", description = "Endpoints for managing prisoner credits")
class CreditResource(
  private val creditService: CreditService,
) {

  @Operation(
    summary = "List credits",
    description = "Returns a paginated list of credits, excluding initial and failed resolutions. " +
      "Each credit includes a computed `status` field derived from the resolution, prison assignment, " +
      "blocked state, and sender information completeness. " +
      "Possible status values: credit_pending, credited, refund_pending, refunded, failed.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of credits with computed status",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CreditPaginatedResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized - requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/")
  fun listCredits(): PaginatedResponse<CreditDto> {
    val credits = creditService.listCompletedCredits()
    val results = credits.map { CreditDto.from(it) }
    return PaginatedResponse(
      count = results.size,
      results = results,
    )
  }
}

@Schema(name = "PaginatedResponseCreditDto", description = "Paginated response containing credit records")
private class CreditPaginatedResponse(
  @Schema(description = "Total number of results", example = "42")
  val count: Int,
  @Schema(description = "URL of the next page, or null if no more pages", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null if on the first page", nullable = true)
  val previous: String?,
  @Schema(description = "List of credit records with computed status")
  val results: List<CreditDto>,
)
