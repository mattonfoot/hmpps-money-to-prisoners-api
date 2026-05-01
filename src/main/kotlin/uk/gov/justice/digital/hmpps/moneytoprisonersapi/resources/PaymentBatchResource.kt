package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_BATCHES
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaymentBatchDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PaymentService
import java.time.LocalDate

/**
 * Payment batches at /batches/ (matching Python's BatchViewSet).
 * This is separate from processing batches at /credits/batches/.
 */
@RestController
@RequestMapping("/batches", produces = ["application/json"])
@SecurityRequirement(name = "oauth2_provider")
@Tag(name = TAG_BATCHES)
class PaymentBatchResource(
  private val paymentService: PaymentService,
) {

  @Operation(summary = "List payment batches")
  @PreAuthorize("hasRole('BANK_ADMIN')")
  @GetMapping("/")
  fun listBatches(
    @RequestParam("date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate? = null,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<PaymentBatchDto> {
    val batches = paymentService.listPaymentBatches(date).map { PaymentBatchDto.from(it) }
    return PaginatedResponse.fromList(batches, limit = limit, offset = offset)
  }
}
