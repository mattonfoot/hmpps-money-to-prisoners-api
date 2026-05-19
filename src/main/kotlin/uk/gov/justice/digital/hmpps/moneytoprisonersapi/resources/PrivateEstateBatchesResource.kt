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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_PRIVATE_ESTATE_BATCHES
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal
import java.time.LocalDate

@RestController
@RequestMapping("/private-estate-batches", produces = ["application/json"])
@SecurityRequirement(name = "oauth2_provider")
@Tag(name = TAG_PRIVATE_ESTATE_BATCHES)
class PrivateEstateBatchesResource(
  private val privateEstateBatchRepository: PrivateEstateBatchRepository,
  private val creditRepository: CreditRepository,
  private val logRepository: LogRepository,
  private val prisonRepository: PrisonRepository,
  private val userRepository: AuthUserRepository,
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
  ): PaginatedResponse<PrivateEstateBatch> {
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
      batches = batches.filter { it.prison?.nomisId == prison }
    }

    val results = batches.map { PrivateEstateBatch.from(it) }
    return PaginatedResponse(count = results.size, results = results)
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
  ): ResponseEntity<PrivateEstateBatch> {
    val prisonEntity = prisonRepository.findById(prison).orElse(null)
      ?: return ResponseEntity.notFound().build()
    val batch = privateEstateBatchRepository.findByPrisonAndDate(prisonEntity, date)
      ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(PrivateEstateBatch.from(batch))
  }

  @Operation(
    summary = "Credit all credit_pending credits in a private estate batch",
    description = "Partial-update endpoint: when the body contains a truthy `credited` flag, " +
      "transitions all credit_pending credits in the batch to credited state and creates a " +
      "CREDITED log entry for each. Returns 204 No Content on success. " +
      "Returns 400 if `credited` is not truthy. Returns 405 for PUT.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Batch processed successfully"),
      ApiResponse(responseCode = "400", description = "`credited` not provided or not truthy"),
      ApiResponse(responseCode = "404", description = "Batch not found"),
      ApiResponse(responseCode = "405", description = "PUT not allowed; use PATCH"),
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
  ): ResponseEntity<Void> {
    val prisonEntity = prisonRepository.findById(prison).orElse(null)
      ?: return ResponseEntity.notFound().build()
    val batch = privateEstateBatchRepository.findByPrisonAndDate(prisonEntity, date)
      ?: return ResponseEntity.notFound().build()

    // Python: `if (request.data or {}).get('credited'):` — anything non-truthy → 400.
    if (!isTruthy(body?.get("credited"))) {
      return ResponseEntity.badRequest().build()
    }

    val owner = userRepository.findByUsername(principal.name)
    for (credit in batch.credits) {
      if (credit.prison != null &&
        !credit.blocked &&
        (credit.resolution == CreditResolution.PENDING.value || credit.resolution == CreditResolution.MANUAL.value)
      ) {
        credit.resolution = CreditResolution.CREDITED.value
        credit.owner = owner
        creditRepository.save(credit)
        val log = Log()
        log.action = LogAction.CREDITED.value
        log.credit = credit
        log.user = owner
        logRepository.save(log)
      }
    }

    return ResponseEntity.noContent().build()
  }

  private fun isTruthy(value: Any?): Boolean = when (value) {
    null -> false
    is Boolean -> value
    is Number -> value.toLong() != 0L
    is String -> value.isNotEmpty() && !value.equals("false", ignoreCase = true) && value != "0"
    is Collection<*> -> value.isNotEmpty()
    is Map<*, *> -> value.isNotEmpty()
    else -> true
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
  ): ResponseEntity<List<Credit>> {
    val prisonEntity = prisonRepository.findById(prison).orElse(null)
      ?: return ResponseEntity.notFound().build()
    val batch = privateEstateBatchRepository.findByPrisonAndDate(prisonEntity, date)
      ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(batch.credits.map { Credit.from(it) })
  }
}
