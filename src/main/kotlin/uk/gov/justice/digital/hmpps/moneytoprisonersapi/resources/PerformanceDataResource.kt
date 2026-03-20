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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PerformanceDataResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PerformanceDataService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@RestController
@RequestMapping(produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Performance", description = "Weekly performance and reporting data")
class PerformanceDataResource(
  private val performanceDataService: PerformanceDataService,
) {

  // -------------------------------------------------------------------------
  // PRF-020 to PRF-024: GET /performance/data/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List weekly performance data",
    description = "Returns weekly aggregated performance metrics including digital take-up, " +
      "completion rate, and user satisfaction. Percentages are returned as formatted strings " +
      "such as \"95%\". Defaults to the last 52 weeks when no date range is supplied (PRF-020 to PRF-024).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Performance data with field headers and weekly results",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PerformanceDataResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires ROLE_SEND_MONEY",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_SEND_MONEY')")
  @GetMapping("/performance/data/")
  fun getPerformanceData(
    @Parameter(description = "Include weeks on or after this date (inclusive)", example = "2023-01-02")
    @RequestParam("week__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    weekGte: LocalDate? = null,
    @Parameter(description = "Include weeks before this date (exclusive)", example = "2024-01-01")
    @RequestParam("week__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    weekLt: LocalDate? = null,
  ): PerformanceDataResponse = performanceDataService.getPerformanceData(weekGte, weekLt)
}
