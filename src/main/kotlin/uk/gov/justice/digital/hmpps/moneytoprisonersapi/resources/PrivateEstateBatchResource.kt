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
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreditDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrivateEstateBatchDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/private-estate-batches", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Private Estate Batches", description = "Endpoints for managing private estate credit batches")
class PrivateEstateBatchResource(
  private val privateEstateBatchRepository: PrivateEstateBatchRepository,
  private val creditRepository: CreditRepository,
  private val logRepository: LogRepository,
) {

  @Operation(
    summary = "List private estate batches",
    description = "Returns a list of private estate batches. Supports filtering by date, date__gte, date__lt, and prison.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "List of private estate batches"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/")
  fun listPrivateEstateBatches(
    @Parameter(description = "Filter by exact date (ISO format)", example = "2024-03-15")
    @RequestParam("date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate? = null,
    @Parameter(description = "Filter by date on or after (ISO format)", example = "2024-03-01")
    @RequestParam("date__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    dateGte: LocalDate? = null,
    @Parameter(description = "Filter by date before (ISO format)", example = "2024-04-01")
    @RequestParam("date__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    dateLt: LocalDate? = null,
    @Parameter(description = "Filter by prison NOMIS ID", example = "PRV")
    @RequestParam("prison")
    prison: String? = null,
  ): List<PrivateEstateBatchDto> {
    var batches = privateEstateBatchRepository.findAll()

    if (date != null) {
      batches = batches.filter { it.date == date }
    }
    if (dateGte != null) {
      batches = batches.filter { !it.date.isBefore(dateGte) }
    }
    if (dateLt != null) {
      batches = batches.filter { it.date.isBefore(dateLt) }
    }
    if (prison != null) {
      batches = batches.filter { it.prison == prison }
    }

    return batches.map { PrivateEstateBatchDto.from(it) }
  }

  @Operation(
    summary = "Get a single private estate batch",
    description = "Returns a single private estate batch by its reference (format: PRISON/YYYY-MM-DD).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "The private estate batch"),
      ApiResponse(responseCode = "404", description = "Batch not found"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{prison}/{date}/")
  fun getPrivateEstateBatch(
    @PathVariable prison: String,
    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
  ): ResponseEntity<PrivateEstateBatchDto> {
    val ref = "$prison/$date"
    val batch = privateEstateBatchRepository.findById(ref).orElse(null)
      ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(PrivateEstateBatchDto.from(batch))
  }

  @Operation(
    summary = "Credit all credit_pending credits in a private estate batch",
    description = "Transitions all credit_pending credits in the batch to credited state. " +
      "Creates a CREDITED log entry for each credit. Returns 200 OK with updated batch.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Batch processed successfully"),
      ApiResponse(responseCode = "404", description = "Batch not found"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{prison}/{date}/")
  @Transactional
  fun patchPrivateEstateBatch(
    @PathVariable prison: String,
    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    @RequestBody(required = false) body: Map<String, Any>?,
    principal: Principal,
  ): ResponseEntity<PrivateEstateBatchDto> {
    val ref = "$prison/$date"
    val batch = privateEstateBatchRepository.findById(ref).orElse(null)
      ?: return ResponseEntity.notFound().build()

    for (credit in batch.credits) {
      if (credit.prison != null &&
        !credit.blocked &&
        (credit.resolution == CreditResolution.PENDING || credit.resolution == CreditResolution.MANUAL)
      ) {
        credit.resolution = CreditResolution.CREDITED
        credit.owner = principal.name
        creditRepository.save(credit)
        logRepository.save(Log(action = LogAction.CREDITED, credit = credit, userId = principal.name))
      }
    }

    return ResponseEntity.ok(PrivateEstateBatchDto.from(batch))
  }

  @Operation(
    summary = "List credits in a private estate batch",
    description = "Returns all credits belonging to the specified private estate batch.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "List of credits in the batch"),
      ApiResponse(responseCode = "404", description = "Batch not found"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{prison}/{date}/credits/")
  fun getPrivateEstateBatchCredits(
    @PathVariable prison: String,
    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
  ): ResponseEntity<List<CreditDto>> {
    val ref = "$prison/$date"
    val batch = privateEstateBatchRepository.findById(ref).orElse(null)
      ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(batch.credits.map { CreditDto.from(it) })
  }
}
