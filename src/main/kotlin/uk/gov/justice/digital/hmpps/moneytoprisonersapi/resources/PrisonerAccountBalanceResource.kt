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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonerAccountBalanceResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/prisoner_account_balances", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Prisoner Account Balances", description = "Endpoints for prisoner account balances")
class PrisonerAccountBalanceResource(
  private val prisonService: PrisonService,
) {

  @Operation(
    summary = "Get prisoner account balance",
    description = "Returns the combined account balance for a prisoner. " +
      "For private estate prisons, balance is looked up from the internal PrisonerBalance table. " +
      "Returns 0 if no balance record is found. Requires ROLE_SEND_MONEY.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner account balance",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonerAccountBalanceResponse::class))],
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
  @GetMapping("/{prisonerNumber}/")
  fun getPrisonerAccountBalance(
    @PathVariable prisonerNumber: String,
  ): PrisonerAccountBalanceResponse {
    val balance = prisonService.getPrisonerAccountBalance(prisonerNumber)
    return PrisonerAccountBalanceResponse(combinedAccountBalance = balance)
  }
}
