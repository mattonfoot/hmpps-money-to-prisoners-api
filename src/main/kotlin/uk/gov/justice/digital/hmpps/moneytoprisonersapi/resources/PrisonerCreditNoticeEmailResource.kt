package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreatePrisonerCreditNoticeEmailRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonerCreditNoticeEmailDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdatePrisonerCreditNoticeEmailRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping("/prisoner_credit_notice_email", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Prisoner Credit Notice Emails", description = "Endpoints for managing prisoner credit notice email configurations")
class PrisonerCreditNoticeEmailResource(
  private val prisonService: PrisonService,
) {

  @Operation(
    summary = "List prisoner credit notice emails",
    description = "Returns a non-paginated list of prisoner credit notice email configurations. " +
      "Requires ROLE_PRISON_CLERK or ROLE_CASHBOOK.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of credit notice email configurations",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CreditNoticeEmailPaginatedResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_PRISON_CLERK or ROLE_CASHBOOK",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK') or hasRole('CASHBOOK')")
  @GetMapping("/")
  fun listCreditNoticeEmails(): PaginatedResponse<PrisonerCreditNoticeEmailDto> {
    val emails = prisonService.listCreditNoticeEmails()
    val results = emails.map { PrisonerCreditNoticeEmailDto.from(it) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(
    summary = "Create prisoner credit notice email",
    description = "Creates a new prisoner credit notice email configuration for a prison. Requires ROLE_PRISON_CLERK.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Email configuration created successfully",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonerCreditNoticeEmailDto::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid prison or email",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_PRISON_CLERK",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK')")
  @PostMapping("/")
  fun createCreditNoticeEmail(
    @Valid @RequestBody request: CreatePrisonerCreditNoticeEmailRequest,
  ): ResponseEntity<PrisonerCreditNoticeEmailDto> {
    val email = prisonService.createCreditNoticeEmail(request.prison, request.email)
    return ResponseEntity.status(HttpStatus.CREATED).body(PrisonerCreditNoticeEmailDto.from(email))
  }

  @Operation(
    summary = "Update prisoner credit notice email",
    description = "Updates the email address for a prison's credit notice configuration. Requires ROLE_PRISON_CLERK.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Email configuration updated successfully",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonerCreditNoticeEmailDto::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request - invalid email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_PRISON_CLERK",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No email configuration found for prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK')")
  @PatchMapping("/{prisonId}/")
  fun updateCreditNoticeEmail(
    @PathVariable prisonId: String,
    @Valid @RequestBody request: UpdatePrisonerCreditNoticeEmailRequest,
  ): PrisonerCreditNoticeEmailDto {
    val email = prisonService.updateCreditNoticeEmail(prisonId, request.email)
    return PrisonerCreditNoticeEmailDto.from(email)
  }
}

@Schema(name = "PaginatedResponsePrisonerCreditNoticeEmailDto", description = "Paginated response containing prisoner credit notice email records")
private class CreditNoticeEmailPaginatedResponse(
  val count: Int,
  val next: String?,
  val previous: String?,
  val results: List<PrisonerCreditNoticeEmailDto>,
)
