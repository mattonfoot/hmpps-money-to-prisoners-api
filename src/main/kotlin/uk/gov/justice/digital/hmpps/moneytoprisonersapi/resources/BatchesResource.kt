package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_BATCHES
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.Batch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateBatchRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ProcessingBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PaymentService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Batch as BatchEntity

@RestController
@RequestMapping(produces = ["application/json"])
@SecurityRequirement(name = "oauth2_provider")
@Tag(name = TAG_BATCHES)
class BatchesResource(
  private val batchRepository: BatchRepository,
  private val creditRepository: CreditRepository,
  private val paymentService: PaymentService,
  private val userRepository: AuthUserRepository,
) {

  @Operation(
    summary = "Create a processing batch",
    description = "Creates a new processing batch containing the specified credits. " +
      "The owner is automatically set to the authenticated user. " +
      "Returns 201 Created with the batch data.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "201", description = "Batch created successfully"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/credits/batches/")
  @ResponseStatus(HttpStatus.CREATED)
  fun createBatch(
    @RequestBody request: CreateBatchRequest,
    principal: Principal,
  ): ProcessingBatch {
    val creditEntities = if (request.creditIds.isNotEmpty()) {
      creditRepository.findAllById(request.creditIds).toMutableSet()
    } else {
      mutableSetOf()
    }
    val owner = userRepository.findByUsername(principal.name)
    val batch = BatchEntity().apply {
      this.user = owner
      this.credits = creditEntities
    }
    return ProcessingBatch.from(batchRepository.save(batch))
  }

  @Operation(
    summary = "List user's processing batches",
    description = "Returns all processing batches owned by the authenticated user.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "List of batches"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/credits/batches/")
  fun listProcessingBatches(
    principal: Principal,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<ProcessingBatch> {
    val batches = batchRepository.findByUserUsername(principal.name).map { ProcessingBatch.from(it) }
    return PaginatedResponse.fromList(batches, limit = limit, offset = offset)
  }

  @Operation(
    summary = "Delete a processing batch",
    description = "Removes a processing batch. Does NOT modify the credits in the batch.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Batch deleted"),
      ApiResponse(responseCode = "404", description = "Batch not found"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/credits/batches/{id}/")
  fun deleteBatch(@PathVariable id: Long): ResponseEntity<Void> {
    if (!batchRepository.existsById(id)) {
      return ResponseEntity.notFound().build()
    }
    batchRepository.deleteById(id)
    return ResponseEntity.noContent().build()
  }

  /**
   * Payment batches at /batches/ (matching Python's BatchViewSet).
   * This is separate from processing batches at /credits/batches/.
   */
  @Operation(summary = "List payment batches")
  @PreAuthorize("hasRole('BANK_ADMIN')")
  @GetMapping("/batches/")
  fun listPaymentBatches(
    @RequestParam("date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    date: LocalDate? = null,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<Batch> {
    val batches = paymentService.listPaymentBatches(date).map { Batch.from(it) }
    return PaginatedResponse.fromList(batches, limit = limit, offset = offset)
  }
}
