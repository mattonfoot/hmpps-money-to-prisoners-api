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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateTransactionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ReconcileTransactionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.RefundTransactionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.TransactionDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.TransactionService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.TransactionStatus
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/transactions", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Transactions", description = "Endpoints for managing bank transfer transactions")
class TransactionResource(
  private val transactionService: TransactionService,
) {

  @Operation(
    summary = "Bulk create transactions",
    description = "Creates an array of bank transfer transaction records. " +
      "For each transaction with category=credit and source=bank_transfer, " +
      "a Credit record is automatically created with source=BANK_TRANSFER and resolution=PENDING. " +
      "Requires ROLE_BANK_ADMIN (TXN-020 to TXN-022).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Transactions created successfully"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires ROLE_BANK_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_BANK_ADMIN')")
  @PostMapping("/")
  @ResponseStatus(HttpStatus.CREATED)
  fun createTransactions(
    @RequestBody requests: List<CreateTransactionRequest>,
  ): List<TransactionDto> {
    val transactions = transactionService.createTransactions(requests)
    return transactions.map { TransactionDto.from(it) }
  }

  @Operation(
    summary = "List transactions",
    description = "Returns a paginated list of transactions. Supports filtering by computed status " +
      "(creditable, refundable, unidentified, anonymous, anomalous), " +
      "received_at date range (gte/lt), and specific IDs (pk). " +
      "Requires ROLE_BANK_ADMIN (TXN-025 to TXN-027).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of transactions",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = TransactionPaginatedResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires ROLE_BANK_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_BANK_ADMIN')")
  @GetMapping("/")
  fun listTransactions(
    @Parameter(description = "Filter by computed status (creditable, refundable, unidentified, anonymous, anomalous)")
    @RequestParam("status")
    statusParam: String? = null,
    @Parameter(description = "Filter transactions received on or after this datetime (inclusive, ISO format)", example = "2024-01-01T00:00:00")
    @RequestParam("received_at__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    receivedAtGte: LocalDateTime? = null,
    @Parameter(description = "Filter transactions received before this datetime (exclusive, ISO format)", example = "2024-02-01T00:00:00")
    @RequestParam("received_at__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    receivedAtLt: LocalDateTime? = null,
    @Parameter(description = "Filter by specific transaction IDs. Pass multiple values (e.g. pk=1&pk=3)")
    @RequestParam("pk")
    pk: List<Long>? = null,
  ): PaginatedResponse<TransactionDto> {
    val status = statusParam?.let { s ->
      TransactionStatus.entries.firstOrNull { it.value == s }
    }
    val transactions = transactionService.listTransactions(
      status = status,
      receivedAtGte = receivedAtGte,
      receivedAtLt = receivedAtLt,
      ids = pk,
    )
    val results = transactions.map { TransactionDto.from(it) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(
    summary = "Bulk refund transactions",
    description = "Marks a list of transactions as refunded by transitioning the linked credit to REFUNDED resolution. " +
      "Only transactions in refundable status are eligible (credit exists, sender info complete, no prison or blocked). " +
      "Returns 409 Conflict if any transaction's credit is not in a valid state for refunding (TXN-023 to TXN-024).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "All transactions refunded successfully"),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request — empty transaction_ids list",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires ROLE_BANK_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Conflict — one or more transactions cannot be refunded",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_BANK_ADMIN')")
  @PatchMapping("/")
  fun refundTransactions(
    @RequestBody request: RefundTransactionRequest,
  ): ResponseEntity<Any> {
    if (request.transactionIds.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    val conflictIds = transactionService.refundTransactions(request.transactionIds)
    return if (conflictIds.isEmpty()) {
      ResponseEntity.noContent().build()
    } else {
      ResponseEntity.status(HttpStatus.CONFLICT)
        .body(mapOf("errors" to listOf("Cannot refund transactions: $conflictIds"), "conflict_ids" to conflictIds))
    }
  }

  @Operation(
    summary = "Reconcile transactions",
    description = "Reconciles transactions within a date range. " +
      "Requires both received_at__gte and received_at__lt date boundaries. " +
      "For transactions linked to credits in private prisons, creates or updates a PrivateEstateBatch. " +
      "Returns 204 if no transactions found, 201 with batch details if transactions reconciled (TXN-028 to TXN-030).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Transactions reconciled and PrivateEstateBatch created"),
      ApiResponse(responseCode = "204", description = "No transactions found in the given date range"),
      ApiResponse(
        responseCode = "400",
        description = "Missing required date boundaries",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden — requires ROLE_BANK_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_BANK_ADMIN')")
  @PostMapping("/reconcile/")
  fun reconcileTransactions(
    @RequestBody request: ReconcileTransactionRequest,
  ): ResponseEntity<Any> {
    if (request.receivedAtGte == null || request.receivedAtLt == null) {
      return ResponseEntity.badRequest().body(mapOf("error" to "Both received_at__gte and received_at__lt are required"))
    }
    val result = transactionService.reconcileTransactions(request.receivedAtGte, request.receivedAtLt)
    return if (result == null) {
      ResponseEntity.noContent().build()
    } else {
      ResponseEntity.status(HttpStatus.CREATED).body(result)
    }
  }
}

@Schema(name = "PaginatedResponseTransactionDto", description = "Paginated response containing transaction records")
private class TransactionPaginatedResponse(
  @Schema(description = "Total number of results", example = "42")
  val count: Int,
  @Schema(description = "URL of the next page, or null if no more pages", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null if on the first page", nullable = true)
  val previous: String?,
  @Schema(description = "List of transaction records")
  val results: List<TransactionDto>,
)
