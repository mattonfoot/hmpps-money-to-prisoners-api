package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.BalanceDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateBalanceRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.BalanceService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.DuplicateBalanceDateException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@RestController
@RequestMapping("/balances", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Balances", description = "Endpoints for managing prisoner account balances")
class BalanceResource(
  private val balanceService: BalanceService,
) {

  @Operation(
    summary = "List balances",
    description = "Returns a paginated list of closing balances, ordered by date descending (newest first). " +
      "Supports filtering by date range using date__lt (exclusive upper bound) and date__gte (inclusive lower bound).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of balances",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BalancePaginatedResponse::class))],
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
  fun listBalances(
    @Parameter(description = "Return balances before this date (exclusive)", example = "2024-01-31")
    @RequestParam("date__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    dateLt: LocalDate? = null,
    @Parameter(description = "Return balances on or after this date (inclusive)", example = "2024-01-01")
    @RequestParam("date__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    dateGte: LocalDate? = null,
  ): PaginatedResponse<BalanceDto> {
    val balances = balanceService.listBalances(dateLt = dateLt, dateGte = dateGte)
    val results = balances.map { BalanceDto.from(it) }
    return PaginatedResponse(
      count = results.size,
      results = results,
    )
  }

  @Operation(
    summary = "Create a balance",
    description = "Creates a new daily closing balance record. Only one balance per date is allowed. " +
      "Requires the ROLE_BANK_ADMIN role.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Balance created successfully",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BalanceDto::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - e.g. duplicate date",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized - requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_BANK_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('BANK_ADMIN')")
  @PostMapping("/")
  fun createBalance(
    @RequestBody request: CreateBalanceRequest,
  ): ResponseEntity<Any> = try {
    val balance = balanceService.createBalance(
      closingBalance = request.closingBalance,
      date = request.date,
    )
    ResponseEntity.status(HttpStatus.CREATED).body(BalanceDto.from(balance))
  } catch (e: DuplicateBalanceDateException) {
    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("detail" to e.message))
  }
}

/**
 * Concrete type for Swagger schema generation, since generics are not well supported by springdoc.
 */
@Schema(name = "PaginatedResponseBalanceDto", description = "Paginated response containing balance records")
private class BalancePaginatedResponse(
  @Schema(description = "Total number of results", example = "42")
  val count: Int,
  @Schema(description = "URL of the next page, or null if no more pages", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null if on the first page", nullable = true)
  val previous: String?,
  @Schema(description = "List of balance records")
  val results: List<BalanceDto>,
)
