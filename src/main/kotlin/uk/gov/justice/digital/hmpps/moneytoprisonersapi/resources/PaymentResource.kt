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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreatePaymentRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaymentBatchDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaymentDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ReconcilePaymentsRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdatePaymentRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PaymentService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/payments", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Payments", description = "Endpoints for managing online prisoner money payments")
class PaymentResource(
  private val paymentService: PaymentService,
) {

  @Operation(
    summary = "Create a payment",
    description = "Creates a new payment with status=pending and a linked credit with resolution=INITIAL and source=ONLINE. " +
      "Required fields: prisoner_number, prisoner_dob, amount. Requires ROLE_SEND_MONEY.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Payment created successfully"),
      ApiResponse(
        responseCode = "400",
        description = "Missing required fields",
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
  @PreAuthorize("hasRole('ROLE_SEND_MONEY')")
  @PostMapping("/")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPayment(
    @RequestBody request: CreatePaymentRequest,
  ): PaymentDto {
    val payment = paymentService.createPayment(request)
    return PaymentDto.from(payment)
  }

  @Operation(
    summary = "Update a payment",
    description = "Partially updates a pending payment (PATCH). " +
      "Only payments in 'pending' status can be updated — returns 409 for non-pending. " +
      "Allowed status transitions: pending -> taken, failed, rejected, expired. " +
      "Card details (cardholder_name, card_number_first_digits, etc.) can be added. " +
      "Billing address is created on first update and updated in-place on subsequent updates. " +
      "received_at can be set explicitly; defaults to now when status=taken. " +
      "Requires ROLE_SEND_MONEY.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Payment updated successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Payment not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Payment cannot be updated because it is not in pending status",
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
  @PreAuthorize("hasRole('ROLE_SEND_MONEY')")
  @PatchMapping("/{uuid}/")
  fun updatePayment(
    @PathVariable uuid: UUID,
    @RequestBody request: UpdatePaymentRequest,
  ): PaymentDto {
    val payment = paymentService.updatePayment(uuid, request)
    return PaymentDto.from(payment)
  }

  @Operation(
    summary = "List pending payments",
    description = "Returns a paginated list of payments with status=pending. " +
      "Supports filtering by modified__lt (ISO datetime). Requires ROLE_SEND_MONEY.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "List of pending payments"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_SEND_MONEY')")
  @GetMapping("/")
  fun listPayments(
    @Parameter(description = "Return only payments modified before this datetime (exclusive)", example = "2024-01-01T00:00:00")
    @RequestParam("modified__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    modifiedLt: LocalDateTime? = null,
  ): PaginatedResponse<PaymentDto> {
    val payments = paymentService.listPendingPayments(modifiedLt = modifiedLt)
    val results = payments.map { PaymentDto.from(it) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(
    summary = "Retrieve a single payment",
    description = "Returns a single payment by UUID. Includes security_check field (null if no check). Requires ROLE_SEND_MONEY.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Payment found"),
      ApiResponse(
        responseCode = "404",
        description = "Payment not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_SEND_MONEY')")
  @GetMapping("/{uuid}/")
  fun getPayment(
    @PathVariable uuid: UUID,
  ): PaymentDto {
    val payment = paymentService.getPayment(uuid)
    return PaymentDto.from(payment)
  }

  @Operation(
    summary = "List payment reconciliation batches",
    description = "Returns all payment reconciliation batches with aggregated payment_amount. " +
      "Supports filtering by settlement_date. Requires ROLE_BANK_ADMIN.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "List of payment batches"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_BANK_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_BANK_ADMIN')")
  @GetMapping("/batches/")
  fun listPaymentBatches(
    @Parameter(description = "Filter by settlement date (exact match)", example = "2024-01-31")
    @RequestParam("date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate? = null,
  ): List<PaymentBatchDto> = paymentService.listPaymentBatches(date).map { PaymentBatchDto.from(it) }

  @Operation(
    summary = "Reconcile taken payments into a batch",
    description = "Creates a payment reconciliation batch from all unreconciled taken payments " +
      "whose credit.received_at falls within the specified date range. " +
      "Marks matched credits as reconciled=true. " +
      "Auto-increments ref_code (max previous + 1, starting at 1). " +
      "Returns 201 with the batch if payments found, or 204 if no payments matched. " +
      "Requires ROLE_BANK_ADMIN.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Batch created"),
      ApiResponse(responseCode = "204", description = "No payments found in date range — no batch created"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden - requires ROLE_BANK_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasRole('ROLE_BANK_ADMIN')")
  @PostMapping("/batches/reconcile/")
  fun reconcilePayments(
    @RequestBody request: ReconcilePaymentsRequest,
  ): ResponseEntity<Any> {
    val batch = paymentService.reconcilePayments(request)
    return if (batch != null) {
      ResponseEntity.status(HttpStatus.CREATED).body(PaymentBatchDto.from(batch))
    } else {
      ResponseEntity.noContent().build()
    }
  }
}
