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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonerValidityResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

@RestController
@RequestMapping("/prisoner_validity", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Prisoner Validity", description = "Endpoints for checking prisoner validity")
class PrisonerValidityResource(
  private val prisonService: PrisonService,
) {

  @Operation(
    summary = "Check prisoner validity",
    description = "Checks whether a prisoner exists with the given prisoner number and date of birth. " +
      "Both prisoner_number and prisoner_dob are required. Requires ROLE_SEND_MONEY.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Validity check result",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonerValidityResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - missing required parameters",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_SEND_MONEY",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('SEND_MONEY')")
  @GetMapping("/")
  fun checkValidity(
    @Parameter(description = "Prisoner number", required = true, example = "A1234BC")
    @RequestParam("prisoner_number")
    prisonerNumber: String,
    @Parameter(description = "Prisoner date of birth (ISO date format)", required = true, example = "1990-01-01")
    @RequestParam("prisoner_dob")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    prisonerDob: LocalDate,
  ): PrisonerValidityResponse {
    val locations = prisonService.checkPrisonerValidity(prisonerNumber, prisonerDob)
    val results = locations.map { mapOf("prisoner_number" to it.prisonerNumber, "prison" to it.prison.nomisId) }
    return PrisonerValidityResponse(count = locations.size, results = results)
  }
}
