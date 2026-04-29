package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_DISBURSEMENTS
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.DisbursementNotFoundException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.DisbursementNotPendingException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementActionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementCommentDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementCommentRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementConfirmItem
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementConfirmRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementComment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementCommentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.DisbursementService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal

@RestController
@RequestMapping("/disbursements", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = TAG_DISBURSEMENTS)
class DisbursementResource(
  private val disbursementService: DisbursementService,
  private val disbursementRepository: DisbursementRepository,
  private val disbursementCommentRepository: DisbursementCommentRepository,
  private val prisonRepository: PrisonRepository,
) {

  private fun prisonNameMap(): Map<String, String> = prisonRepository.findAll().associate { it.nomisId to it.name }

  @Operation(
    summary = "List disbursements",
    description = "Returns a paginated list of disbursements. " +
      "Supports filtering by amount, resolution, method, prisoner details, " +
      "recipient name, prison, bank details, postcode, and date ranges. " +
      "Ordering by created, amount, resolution, method, prisoner_name, recipient_name.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of disbursements",
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
  fun listDisbursements(
    @Parameter(description = "Filter by exact amount in pence", example = "5000")
    @RequestParam("amount") amount: Long? = null,
    @Parameter(description = "Filter by minimum amount (inclusive) in pence", example = "1000")
    @RequestParam("amount__gte") amountGte: Long? = null,
    @Parameter(description = "Filter by maximum amount (inclusive) in pence", example = "10000")
    @RequestParam("amount__lte") amountLte: Long? = null,
    @Parameter(description = "Filter by resolution (multiple values allowed)")
    @RequestParam("resolution") resolution: List<DisbursementResolution>? = null,
    @Parameter(description = "Filter by payment method")
    @RequestParam("method") method: DisbursementMethod? = null,
    @Parameter(description = "Filter by prisoner number (case-insensitive exact)", example = "A1234BC")
    @RequestParam("prisoner_number") prisonerNumber: String? = null,
    @Parameter(description = "Filter by prisoner name (case-insensitive substring)", example = "Smith")
    @RequestParam("prisoner_name") prisonerName: String? = null,
    @Parameter(description = "Filter by recipient name (case-insensitive substring)", example = "Doe")
    @RequestParam("recipient_name") recipientName: String? = null,
    @Parameter(description = "Filter by prison NOMIS ID(s)", example = "LEI")
    @RequestParam("prison") prison: List<String>? = null,
    @Parameter(description = "Filter by sort code (exact)", example = "112233")
    @RequestParam("sort_code") sortCode: String? = null,
    @Parameter(description = "Filter by account number (exact)", example = "12345678")
    @RequestParam("account_number") accountNumber: String? = null,
    @Parameter(description = "Filter by roll number (exact)", example = "ROLL001")
    @RequestParam("roll_number") rollNumber: String? = null,
    @Parameter(description = "Filter by postcode (normalized: remove spaces, uppercase)", example = "SW1A 1AA")
    @RequestParam("postcode") postcode: String? = null,
    @Parameter(description = "Filter by city (case-insensitive substring)")
    @RequestParam("city") city: String? = null,
    @Parameter(description = "Filter by recipient email (case-insensitive substring)")
    @RequestParam("recipient_email") recipientEmail: String? = null,
    @Parameter(description = "Filter by recipient is company flag")
    @RequestParam("recipient_is_company") recipientIsCompany: Boolean? = null,
    @Parameter(description = "Filter by invoice number (exact)")
    @RequestParam("invoice_number") invoiceNumber: String? = null,
    @Parameter(description = "Filter by NOMIS transaction ID (exact)")
    @RequestParam("nomis_transaction_id") nomisTransactionId: String? = null,
    @Parameter(description = "Filter by exact created date")
    @RequestParam("created") created: String? = null,
    @Parameter(description = "Filter by created on or after (inclusive)")
    @RequestParam("created__gte") createdGte: String? = null,
    @Parameter(description = "Filter by created before (exclusive)")
    @RequestParam("created__lt") createdLt: String? = null,
    @Parameter(description = "Filter by log creation date on or after")
    @RequestParam("logged_at__gte") loggedAtGte: String? = null,
    @Parameter(description = "Filter by log creation date before")
    @RequestParam("logged_at__lt") loggedAtLt: String? = null,
    @Parameter(description = "Filter by log action")
    @RequestParam("log__action") logAction: String? = null,
    @Parameter(description = "Filter by prison region (case-insensitive substring)")
    @RequestParam("prison_region") prisonRegion: String? = null,
    @Parameter(description = "Filter by prison category name")
    @RequestParam("prison_category") prisonCategory: String? = null,
    @Parameter(description = "Filter by prison population type")
    @RequestParam("prison_population") prisonPopulation: String? = null,
    @Parameter(description = "Simple search across recipient and prisoner names")
    @RequestParam("simple_search") simpleSearch: String? = null,
    @Parameter(description = "Filter by amount ending with suffix")
    @RequestParam("amount__endswith") amountEndswith: String? = null,
    @Parameter(description = "Filter by amount matching regex")
    @RequestParam("amount__regex") amountRegex: String? = null,
    @Parameter(description = "Exclude amounts ending with suffix")
    @RequestParam("exclude_amount__endswith") excludeAmountEndswith: String? = null,
    @Parameter(description = "Exclude amounts matching regex")
    @RequestParam("exclude_amount__regex") excludeAmountRegex: String? = null,
    @Parameter(description = "Order results by field. Allowed: created, amount, resolution, method, prisoner_name, recipient_name. Prefix with - for descending.", example = "-created")
    @RequestParam("ordering") ordering: String? = null,
    @Parameter(description = "Filter disbursements for prisoners monitored by the current user")
    @RequestParam("monitored") monitored: Boolean? = null,
    @Parameter(description = "Number of results to return per page.", example = "20")
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @Parameter(description = "The initial index from which to return the results.", example = "0")
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
    principal: Principal,
  ): PaginatedResponse<DisbursementDto> {
    val disbursements = disbursementService.listDisbursements(
      amount = amount,
      amountGte = amountGte,
      amountLte = amountLte,
      amountEndswith = amountEndswith,
      amountRegex = amountRegex,
      excludeAmountEndswith = excludeAmountEndswith,
      excludeAmountRegex = excludeAmountRegex,
      resolution = resolution,
      method = method,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      recipientName = recipientName,
      recipientEmail = recipientEmail,
      recipientIsCompany = recipientIsCompany,
      prisons = prison,
      prisonRegion = prisonRegion,
      prisonCategory = prisonCategory,
      prisonPopulation = prisonPopulation,
      sortCode = sortCode,
      accountNumber = accountNumber,
      rollNumber = rollNumber,
      postcode = postcode,
      city = city,
      invoiceNumber = invoiceNumber,
      nomisTransactionId = nomisTransactionId,
      created = created?.let { java.time.LocalDateTime.parse(it + "T00:00:00") },
      createdGte = createdGte?.let { java.time.LocalDateTime.parse(it + "T00:00:00") },
      createdLt = createdLt?.let { java.time.LocalDateTime.parse(it + "T00:00:00") },
      loggedAtGte = loggedAtGte?.let { java.time.LocalDateTime.parse(it + "T00:00:00") },
      loggedAtLt = loggedAtLt?.let { java.time.LocalDateTime.parse(it + "T00:00:00") },
      logAction = logAction,
      simpleSearch = simpleSearch,
      ordering = ordering,
      monitoredByUsername = if (monitored == true) principal.name else null,
    )
    val nameMap = prisonNameMap()
    val results = disbursements.map { DisbursementDto.from(it, nameMap) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Get a single disbursement by ID")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{id}/")
  fun getDisbursement(@PathVariable id: Long): ResponseEntity<DisbursementDto> {
    val disbursement = disbursementService.getDisbursement(id)
      ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(DisbursementDto.from(disbursement, prisonNameMap()))
  }

  @Operation(
    summary = "Create a disbursement",
    description = "Creates a new disbursement with PENDING resolution. Requires ROLE_PRISON_CLERK.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Disbursement created successfully",
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
  @ResponseStatus(HttpStatus.CREATED)
  fun createDisbursement(
    @RequestBody request: CreateDisbursementRequest,
    principal: Principal,
  ): DisbursementDto {
    val disbursement = disbursementService.createDisbursement(request, principal.name)
    return DisbursementDto.from(disbursement, prisonNameMap())
  }

  @Operation(
    summary = "Partially update a disbursement",
    description = "Updates a PENDING disbursement. Resolution field is read-only. Returns 400 if disbursement is not PENDING.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Disbursement updated successfully",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Request - disbursement is not in PENDING state",
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
        description = "Not Found - disbursement not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK')")
  @RequestMapping(value = ["/{id}/"], method = [RequestMethod.PATCH, RequestMethod.PUT])
  fun updateDisbursement(
    @PathVariable id: Long,
    @RequestBody request: UpdateDisbursementRequest,
    principal: Principal,
  ): ResponseEntity<Any> = try {
    val updated = disbursementService.updateDisbursement(id, request, principal.name)
    ResponseEntity.ok(DisbursementDto.from(updated, prisonNameMap()))
  } catch (_: DisbursementNotFoundException) {
    ResponseEntity.notFound().build()
  } catch (_: DisbursementNotPendingException) {
    ResponseEntity.badRequest().build()
  }

  @Operation(
    summary = "Reject disbursements",
    description = "Transitions disbursements to REJECTED state. All-or-nothing: if any transition is invalid, returns 409.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Disbursements rejected successfully"),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "409", description = "Conflict - one or more disbursements cannot be transitioned", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK')")
  @PostMapping("/actions/reject/")
  fun reject(
    @RequestBody request: DisbursementActionRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    disbursementService.reject(request, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(
    summary = "Pre-confirm disbursements",
    description = "Transitions disbursements to PRECONFIRMED state. All-or-nothing: if any transition is invalid, returns 409.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Disbursements pre-confirmed successfully"),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "409", description = "Conflict - one or more disbursements cannot be transitioned", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK')")
  @PostMapping("/actions/preconfirm/")
  fun preconfirm(
    @RequestBody request: DisbursementActionRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    disbursementService.preconfirm(request, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(
    summary = "Reset disbursements to PENDING",
    description = "Transitions disbursements back to PENDING state. All-or-nothing: if any transition is invalid, returns 409.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Disbursements reset to PENDING successfully"),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "409", description = "Conflict - one or more disbursements cannot be transitioned", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK')")
  @PostMapping("/actions/reset/")
  fun reset(
    @RequestBody request: DisbursementActionRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    disbursementService.reset(request, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(
    summary = "Confirm disbursements",
    description = "Transitions disbursements to CONFIRMED state and generates invoice numbers. All-or-nothing: if any transition is invalid, returns 409.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Disbursements confirmed successfully"),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "409", description = "Conflict - one or more disbursements cannot be transitioned", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK')")
  @PostMapping("/actions/confirm/")
  fun confirm(
    @RequestBody items: List<DisbursementConfirmItem>,
    principal: Principal,
  ): ResponseEntity<Any> {
    disbursementService.confirm(DisbursementConfirmRequest(items), principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(
    summary = "Send disbursements",
    description = "Transitions disbursements to SENT state (terminal). All-or-nothing: if any transition is invalid, returns 409. Requires ROLE_BANK_ADMIN.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Disbursements sent successfully"),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "403", description = "Forbidden - requires ROLE_BANK_ADMIN", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "409", description = "Conflict - one or more disbursements cannot be transitioned", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("hasRole('DISBURSEMENT_BANK_ADMIN')")
  @PostMapping("/actions/send/")
  fun send(
    @RequestBody request: DisbursementActionRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    disbursementService.send(request, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(
    summary = "Create comments on disbursements",
    description = "Creates one or more comments on disbursement records. " +
      "The user (userId) is automatically set to the authenticated user. " +
      "Comment text is limited to 3000 characters. " +
      "Accepts an array of comment objects. " +
      "Returns 201 Created with the created comment data. Requires ROLE_PRISON_CLERK.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Comments created successfully"),
      ApiResponse(responseCode = "400", description = "Invalid request — comment exceeds 3000 characters", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "401", description = "Unauthorized", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
      ApiResponse(responseCode = "403", description = "Forbidden - requires ROLE_PRISON_CLERK", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]),
    ],
  )
  @PreAuthorize("hasRole('PRISON_CLERK')")
  @PostMapping("/comments/")
  @ResponseStatus(HttpStatus.CREATED)
  fun createComments(
    @RequestBody @Valid requests: List<DisbursementCommentRequest>,
    principal: Principal,
  ): List<DisbursementCommentDto> {
    val created = requests.map { request ->
      val disbursement = disbursementRepository.findById(request.disbursement).orElse(null)
      val comment = DisbursementComment(
        comment = request.comment,
        category = request.category,
        disbursement = disbursement,
        userId = principal.name,
      )
      disbursementCommentRepository.save(comment)
    }
    return created.map { DisbursementCommentDto.from(it) }
  }
}
