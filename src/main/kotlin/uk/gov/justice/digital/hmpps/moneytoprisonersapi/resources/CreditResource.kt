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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreditActionItem
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreditActionResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreditDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ProcessedCreditGroupDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.RefundRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ReviewRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SetManualRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal
import java.time.LocalDateTime

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
      "Supports filtering by status, prison (single, multiple, region, category, population), " +
      "amount (exact, range, endswith, regex, and exclusions), prisoner details, resolution, review state, received date range, owner, validity, " +
      "sender/payment details (name, sort code, account number, roll number, email, IP address, card details, postcode, payment reference), source type, " +
      "log creation date range, security check presence and actioned state, credit ID inclusion/exclusion, and monitored profile linkage. " +
      "Full-text search across prisoner_name, prisoner_number, sender_name, amount (£nn.nn format), and payment UUID prefix. " +
      "Simple search across transaction sender_name, payment cardholder_name, payment email, and prisoner_number. " +
      "Ordering by created, received_at, amount, prisoner_number, prisoner_name (prefix with - for descending). " +
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
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires VIEW_CREDIT permission and a valid client (Cashbook, NomsOps, or BankAdmin)",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/")
  fun listCredits(
    @Parameter(
      description = "Full-text search across prisoner_name, prisoner_number, sender_name, " +
        "amount (£nn.nn format), and payment UUID prefix (8 chars). All words must match (AND logic).",
      example = "john smith",
    )
    @RequestParam("search")
    search: String? = null,
    @Parameter(
      description = "Simple search across transaction sender_name, payment cardholder_name, " +
        "payment email, and prisoner_number (case-insensitive substring match)",
      example = "alice",
    )
    @RequestParam("simple_search")
    simpleSearch: String? = null,
    @Parameter(
      description = "Order results by field. Allowed fields: created, received_at, amount, " +
        "prisoner_number, prisoner_name. Prefix with - for descending order.",
      example = "-created",
    )
    @RequestParam("ordering")
    ordering: String? = null,
    @Parameter(description = "Filter by computed status (credit_pending, credited, refund_pending, refunded, failed)")
    @RequestParam("status")
    status: CreditStatus? = null,
    @Parameter(description = "Filter by prison NOMIS ID(s). Pass once for exact match, repeat for multiple (e.g. prison=LEI&prison=MDI)", example = "LEI")
    @RequestParam("prison")
    prison: List<String>? = null,
    @Parameter(description = "Filter for credits with no prison assigned")
    @RequestParam("prison__isnull")
    prisonIsNull: Boolean? = null,
    @Parameter(description = "Filter by prison region (case-insensitive substring match)", example = "Yorkshire")
    @RequestParam("prison_region")
    prisonRegion: String? = null,
    @Parameter(description = "Filter by prison category name (matches any category assigned to the prison)", example = "Category B")
    @RequestParam("prison_category")
    prisonCategory: String? = null,
    @Parameter(description = "Filter by prison population type (matches any population assigned to the prison)", example = "Adult")
    @RequestParam("prison_population")
    prisonPopulation: String? = null,
    @Parameter(description = "Filter by exact amount in pence", example = "5000")
    @RequestParam("amount")
    amount: Long? = null,
    @Parameter(description = "Filter by minimum amount (inclusive) in pence", example = "1000")
    @RequestParam("amount__gte")
    amountGte: Long? = null,
    @Parameter(description = "Filter by maximum amount (inclusive) in pence", example = "10000")
    @RequestParam("amount__lte")
    amountLte: Long? = null,
    @Parameter(description = "Filter by last digits of amount in pence (endswith match)", example = "50")
    @RequestParam("amount__endswith")
    amountEndswith: String? = null,
    @Parameter(description = "Filter by regex pattern on amount in pence", example = "^1.*")
    @RequestParam("amount__regex")
    amountRegex: String? = null,
    @Parameter(description = "Exclude credits where amount ends with the given suffix", example = "00")
    @RequestParam("exclude_amount__endswith")
    excludeAmountEndswith: String? = null,
    @Parameter(description = "Exclude credits where amount matches the given regex pattern", example = "^1.*")
    @RequestParam("exclude_amount__regex")
    excludeAmountRegex: String? = null,
    @Parameter(description = "Filter by prisoner name (case-insensitive substring match)", example = "Smith")
    @RequestParam("prisoner_name")
    prisonerName: String? = null,
    @Parameter(description = "Filter by prisoner number (exact match)", example = "A1234BC")
    @RequestParam("prisoner_number")
    prisonerNumber: String? = null,
    @Parameter(description = "Filter by credit owner/user", example = "clerk1")
    @RequestParam("user")
    user: String? = null,
    @Parameter(description = "Filter by resolution status (exact match)")
    @RequestParam("resolution")
    resolution: CreditResolution? = null,
    @Parameter(description = "Filter by reviewed flag")
    @RequestParam("reviewed")
    reviewed: Boolean? = null,
    @Parameter(description = "Filter credits received on or after this datetime (inclusive, ISO format)", example = "2024-01-01T00:00:00")
    @RequestParam("received_at__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    receivedAtGte: LocalDateTime? = null,
    @Parameter(description = "Filter credits received before this datetime (exclusive, ISO format)", example = "2024-02-01T00:00:00")
    @RequestParam("received_at__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    receivedAtLt: LocalDateTime? = null,
    @Parameter(description = "Filter by validity: true = credit_pending or credited, false = all others")
    @RequestParam("valid")
    valid: Boolean? = null,
    @Parameter(description = "Filter by sender name (case-insensitive substring match on transaction sender_name or payment cardholder_name)", example = "Smith")
    @RequestParam("sender_name")
    senderName: String? = null,
    @Parameter(description = "Filter by sender sort code (exact match on transaction field)", example = "112233")
    @RequestParam("sender_sort_code")
    senderSortCode: String? = null,
    @Parameter(description = "Filter by sender account number (exact match on transaction field)", example = "12345678")
    @RequestParam("sender_account_number")
    senderAccountNumber: String? = null,
    @Parameter(description = "Filter by sender roll number (exact match on transaction field)", example = "ROLL001")
    @RequestParam("sender_roll_number")
    senderRollNumber: String? = null,
    @Parameter(description = "Filter for credits with blank sender name from transactions")
    @RequestParam("sender_name__isblank")
    senderNameIsBlank: Boolean? = null,
    @Parameter(description = "Filter for credits with blank sender sort code from transactions")
    @RequestParam("sender_sort_code__isblank")
    senderSortCodeIsBlank: Boolean? = null,
    @Parameter(description = "Filter by sender email (case-insensitive substring match on payment email)", example = "john@example.com")
    @RequestParam("sender_email")
    senderEmail: String? = null,
    @Parameter(description = "Filter by sender IP address (exact match on payment field)", example = "192.168.1.1")
    @RequestParam("sender_ip_address")
    senderIpAddress: String? = null,
    @Parameter(description = "Filter by card number first digits (exact match on payment field)", example = "411111")
    @RequestParam("card_number_first_digits")
    cardNumberFirstDigits: String? = null,
    @Parameter(description = "Filter by card number last digits (exact match on payment field)", example = "1234")
    @RequestParam("card_number_last_digits")
    cardNumberLastDigits: String? = null,
    @Parameter(description = "Filter by card expiry date (exact match on payment field)", example = "12/25")
    @RequestParam("card_expiry_date")
    cardExpiryDate: String? = null,
    @Parameter(description = "Filter by sender postcode (normalized matching, ignores spaces and case, on payment billing address)", example = "SW1A 1AA")
    @RequestParam("sender_postcode")
    senderPostcode: String? = null,
    @Parameter(description = "Filter by payment reference (prefix match on first 8 chars of payment UUID)", example = "abcdef12")
    @RequestParam("payment_reference")
    paymentReference: String? = null,
    @Parameter(description = "Filter by credit source type: bank_transfer (has transaction), online (has payment), unknown (neither)")
    @RequestParam("source")
    source: CreditSource? = null,
    @Parameter(description = "Filter by log creation date on or after this datetime (truncated to UTC date, inclusive)", example = "2024-01-01T00:00:00")
    @RequestParam("logged_at__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    loggedAtGte: LocalDateTime? = null,
    @Parameter(description = "Filter by log creation date before this datetime (truncated to UTC date, exclusive)", example = "2024-02-01T00:00:00")
    @RequestParam("logged_at__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    loggedAtLt: LocalDateTime? = null,
    @Parameter(description = "Filter by presence of security check: true = no check exists, false = check exists")
    @RequestParam("security_check__isnull")
    securityCheckIsnull: Boolean? = null,
    @Parameter(description = "Filter by security check actioned state: true = not yet actioned, false = has been actioned")
    @RequestParam("security_check__actioned_by__isnull")
    securityCheckActionedByIsnull: Boolean? = null,
    @Parameter(description = "Exclude specific credit IDs from results. Pass multiple values (e.g. exclude_credit__in=1&exclude_credit__in=2)")
    @RequestParam("exclude_credit__in")
    excludeCreditIn: List<Long>? = null,
    @Parameter(description = "Filter for credits linked to monitored sender or prisoner profiles")
    @RequestParam("monitored")
    monitored: Boolean? = null,
    @Parameter(description = "Filter by specific credit IDs. Pass multiple values (e.g. pk=1&pk=3)")
    @RequestParam("pk")
    pk: List<Long>? = null,
  ): PaginatedResponse<CreditDto> {
    val credits = creditService.listCredits(
      search = search,
      simpleSearch = simpleSearch,
      ordering = ordering,
      status = status,
      prisons = prison,
      prisonIsNull = prisonIsNull,
      prisonRegion = prisonRegion,
      prisonCategory = prisonCategory,
      prisonPopulation = prisonPopulation,
      amount = amount,
      amountGte = amountGte,
      amountLte = amountLte,
      amountEndswith = amountEndswith,
      amountRegex = amountRegex,
      excludeAmountEndswith = excludeAmountEndswith,
      excludeAmountRegex = excludeAmountRegex,
      prisonerName = prisonerName,
      prisonerNumber = prisonerNumber,
      user = user,
      resolution = resolution,
      reviewed = reviewed,
      receivedAtGte = receivedAtGte,
      receivedAtLt = receivedAtLt,
      valid = valid,
      senderName = senderName,
      senderSortCode = senderSortCode,
      senderAccountNumber = senderAccountNumber,
      senderRollNumber = senderRollNumber,
      senderNameIsBlank = senderNameIsBlank,
      senderSortCodeIsBlank = senderSortCodeIsBlank,
      senderEmail = senderEmail,
      senderIpAddress = senderIpAddress,
      cardNumberFirstDigits = cardNumberFirstDigits,
      cardNumberLastDigits = cardNumberLastDigits,
      cardExpiryDate = cardExpiryDate,
      senderPostcode = senderPostcode,
      paymentReference = paymentReference,
      source = source,
      loggedAtGte = loggedAtGte,
      loggedAtLt = loggedAtLt,
      securityCheckIsnull = securityCheckIsnull,
      securityCheckActionedByIsnull = securityCheckActionedByIsnull,
      excludeCreditIn = excludeCreditIn,
      monitored = monitored,
      pk = pk,
    )
    val results = credits.map { CreditDto.from(it) }
    return PaginatedResponse(
      count = results.size,
      results = results,
    )
  }

  @Operation(
    summary = "Set credits to manual resolution",
    description = "Transitions a list of credits from pending to manual resolution. " +
      "Only credits with resolution=pending are eligible. " +
      "Credits not in pending state are returned as conflict_ids. " +
      "Sets resolution=manual and owner=requesting user on each eligible credit. " +
      "Creates a log entry with LogAction.MANUAL for each transitioned credit. " +
      "Returns 204 No Content when all provided credits are processed. " +
      "Returns 200 OK with conflict_ids when some credits were not in pending state. " +
      "Requires NomsOpsClientIDPermissions (CRD-120).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "All credits transitioned to manual with no conflicts",
      ),
      ApiResponse(
        responseCode = "200",
        description = "Request processed but some credits were not in pending state",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CreditActionResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request — empty credit_ids list",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized — requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires NomsOps client",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/actions/setmanual/")
  fun setManual(
    @RequestBody request: SetManualRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    if (request.creditIds.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    val conflictIds = creditService.setManual(request.creditIds, principal.name)
    return if (conflictIds.isEmpty()) {
      ResponseEntity.noContent().build()
    } else {
      ResponseEntity.ok(CreditActionResponse(conflictIds))
    }
  }

  @Operation(
    summary = "Review credits",
    description = "Marks a list of credits as reviewed by security staff. " +
      "Sets reviewed=true on all specified credits regardless of their current state. " +
      "Creates a log entry with LogAction.REVIEWED for each credit. " +
      "Returns 204 No Content on success. " +
      "Requires NomsOpsClientIDPermissions (CRD-134) and review_credit permission (CRD-135).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "All specified credits marked as reviewed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request — empty credit_ids list",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized — requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires NomsOps client and review_credit permission",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/actions/review/")
  fun review(
    @RequestBody request: ReviewRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    if (request.creditIds.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    creditService.review(request.creditIds, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(
    summary = "Credit prisoners",
    description = "Credits a list of prisoners by transitioning eligible credits from credit_pending to credited state. " +
      "Only credits in credit_pending state (prison assigned, pending or manual resolution, not blocked) are eligible. " +
      "Credits not in credit_pending state are returned as conflict_ids. " +
      "Returns 204 No Content when all provided credits are processed. " +
      "Returns 200 OK with conflict_ids when some credits were not in credit_pending state. " +
      "Items with credited=false are skipped without error. " +
      "Requires CashbookClientIDPermissions (CRD-116) and credit_credit permission (CRD-117).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "All credits processed successfully with no conflicts",
      ),
      ApiResponse(
        responseCode = "200",
        description = "Request processed but some credits were not in credit_pending state",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CreditActionResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request — empty list or missing required fields",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized — requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires Cashbook client and credit_credit permission",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/actions/credit/")
  fun creditPrisoners(
    @RequestBody items: List<CreditActionItem>,
    principal: Principal,
  ): ResponseEntity<Any> {
    if (items.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    val conflictIds = creditService.creditPrisoners(items, principal.name)
    return if (conflictIds.isEmpty()) {
      ResponseEntity.noContent().build()
    } else {
      ResponseEntity.ok(CreditActionResponse(conflictIds))
    }
  }

  @Operation(
    summary = "List credits grouped by credited date and owner",
    description = "Returns credits grouped by the date of their CREDITED log entry and owner. " +
      "Only includes credits that have a LogAction.CREDITED log entry. " +
      "Supports all the same filter parameters as GET /credits/. " +
      "Ordered by logged_at date descending. " +
      "Each group contains logged_at (date), owner (username), owner_name, count, total, comment_count.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of processed credit groups",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized — requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/processed/")
  fun listProcessedCredits(
    @RequestParam("search") search: String? = null,
    @RequestParam("simple_search") simpleSearch: String? = null,
    @RequestParam("status") status: CreditStatus? = null,
    @RequestParam("prison") prison: List<String>? = null,
    @RequestParam("prison__isnull") prisonIsNull: Boolean? = null,
    @RequestParam("prison_region") prisonRegion: String? = null,
    @RequestParam("prison_category") prisonCategory: String? = null,
    @RequestParam("prison_population") prisonPopulation: String? = null,
    @RequestParam("amount") amount: Long? = null,
    @RequestParam("amount__gte") amountGte: Long? = null,
    @RequestParam("amount__lte") amountLte: Long? = null,
    @RequestParam("amount__endswith") amountEndswith: String? = null,
    @RequestParam("amount__regex") amountRegex: String? = null,
    @RequestParam("exclude_amount__endswith") excludeAmountEndswith: String? = null,
    @RequestParam("exclude_amount__regex") excludeAmountRegex: String? = null,
    @RequestParam("prisoner_name") prisonerName: String? = null,
    @RequestParam("prisoner_number") prisonerNumber: String? = null,
    @RequestParam("user") user: String? = null,
    @RequestParam("resolution") resolution: CreditResolution? = null,
    @RequestParam("reviewed") reviewed: Boolean? = null,
    @RequestParam("received_at__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    receivedAtGte: LocalDateTime? = null,
    @RequestParam("received_at__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    receivedAtLt: LocalDateTime? = null,
    @RequestParam("valid") valid: Boolean? = null,
    @RequestParam("sender_name") senderName: String? = null,
    @RequestParam("sender_sort_code") senderSortCode: String? = null,
    @RequestParam("sender_account_number") senderAccountNumber: String? = null,
    @RequestParam("sender_roll_number") senderRollNumber: String? = null,
    @RequestParam("sender_name__isblank") senderNameIsBlank: Boolean? = null,
    @RequestParam("sender_sort_code__isblank") senderSortCodeIsBlank: Boolean? = null,
    @RequestParam("sender_email") senderEmail: String? = null,
    @RequestParam("sender_ip_address") senderIpAddress: String? = null,
    @RequestParam("card_number_first_digits") cardNumberFirstDigits: String? = null,
    @RequestParam("card_number_last_digits") cardNumberLastDigits: String? = null,
    @RequestParam("card_expiry_date") cardExpiryDate: String? = null,
    @RequestParam("sender_postcode") senderPostcode: String? = null,
    @RequestParam("payment_reference") paymentReference: String? = null,
    @RequestParam("source") source: CreditSource? = null,
    @RequestParam("logged_at__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    loggedAtGte: LocalDateTime? = null,
    @RequestParam("logged_at__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    loggedAtLt: LocalDateTime? = null,
    @RequestParam("security_check__isnull") securityCheckIsnull: Boolean? = null,
    @RequestParam("security_check__actioned_by__isnull") securityCheckActionedByIsnull: Boolean? = null,
    @RequestParam("exclude_credit__in") excludeCreditIn: List<Long>? = null,
    @RequestParam("monitored") monitored: Boolean? = null,
    @RequestParam("pk") pk: List<Long>? = null,
  ): List<ProcessedCreditGroupDto> = creditService.listProcessedCredits(
    search = search,
    simpleSearch = simpleSearch,
    status = status,
    prisons = prison,
    prisonIsNull = prisonIsNull,
    prisonRegion = prisonRegion,
    prisonCategory = prisonCategory,
    prisonPopulation = prisonPopulation,
    amount = amount,
    amountGte = amountGte,
    amountLte = amountLte,
    amountEndswith = amountEndswith,
    amountRegex = amountRegex,
    excludeAmountEndswith = excludeAmountEndswith,
    excludeAmountRegex = excludeAmountRegex,
    prisonerName = prisonerName,
    prisonerNumber = prisonerNumber,
    user = user,
    resolution = resolution,
    reviewed = reviewed,
    receivedAtGte = receivedAtGte,
    receivedAtLt = receivedAtLt,
    valid = valid,
    senderName = senderName,
    senderSortCode = senderSortCode,
    senderAccountNumber = senderAccountNumber,
    senderRollNumber = senderRollNumber,
    senderNameIsBlank = senderNameIsBlank,
    senderSortCodeIsBlank = senderSortCodeIsBlank,
    senderEmail = senderEmail,
    senderIpAddress = senderIpAddress,
    cardNumberFirstDigits = cardNumberFirstDigits,
    cardNumberLastDigits = cardNumberLastDigits,
    cardExpiryDate = cardExpiryDate,
    senderPostcode = senderPostcode,
    paymentReference = paymentReference,
    source = source,
    loggedAtGte = loggedAtGte,
    loggedAtLt = loggedAtLt,
    securityCheckIsnull = securityCheckIsnull,
    securityCheckActionedByIsnull = securityCheckActionedByIsnull,
    excludeCreditIn = excludeCreditIn,
    monitored = monitored,
    pk = pk,
  )

  @Operation(
    summary = "Refund credits",
    description = "Marks a list of credits as refunded. " +
      "Only credits in refund_pending status are eligible: " +
      "(no prison assigned OR blocked) AND pending resolution AND sender info complete. " +
      "Sets resolution=refunded (terminal state) on each eligible credit. " +
      "Creates a log entry with LogAction.REFUNDED for each credit. " +
      "Returns 204 No Content on success. " +
      "Returns 409 Conflict if any credit is not in refund_pending state (strict validation). " +
      "Requires authentication (CRD-140).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "All specified credits successfully refunded",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request — empty credit_ids list",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized — requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires authenticated access",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Conflict — one or more credits are not in refund_pending state",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/actions/refund/")
  fun refund(
    @RequestBody request: RefundRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    if (request.creditIds.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    creditService.refund(request.creditIds, principal.name)
    return ResponseEntity.noContent().build()
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
