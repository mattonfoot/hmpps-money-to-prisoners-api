package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.BatchDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateBatchRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Batch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal

@RestController
@RequestMapping("/batches", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Batches", description = "Endpoints for managing processing batches")
class BatchResource(
  private val batchRepository: BatchRepository,
  private val creditRepository: CreditRepository,
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
  @PostMapping("/")
  @ResponseStatus(HttpStatus.CREATED)
  fun createBatch(
    @RequestBody request: CreateBatchRequest,
    principal: Principal,
  ): BatchDto {
    val credits = if (request.creditIds.isNotEmpty()) {
      creditRepository.findAllById(request.creditIds).toMutableList()
    } else {
      mutableListOf()
    }
    val batch = Batch(owner = principal.name, credits = credits)
    return BatchDto.from(batchRepository.save(batch))
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
  @GetMapping("/")
  fun listBatches(principal: Principal): List<BatchDto> = batchRepository.findByOwner(principal.name).map { BatchDto.from(it) }

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
  @DeleteMapping("/{id}/")
  fun deleteBatch(@PathVariable id: Long): ResponseEntity<Void> {
    if (!batchRepository.existsById(id)) {
      return ResponseEntity.notFound().build()
    }
    batchRepository.deleteById(id)
    return ResponseEntity.noContent().build()
  }
}
