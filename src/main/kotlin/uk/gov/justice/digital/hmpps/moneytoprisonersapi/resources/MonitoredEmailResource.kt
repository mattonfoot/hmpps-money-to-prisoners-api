package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_MONITORED
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
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.MonitoredEmailDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MonitoredPartialEmailAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MonitoredPartialEmailAddressRepository

@RestController
@RequestMapping("/security/monitored-email-addresses", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = TAG_MONITORED)
class MonitoredEmailResource(
  private val repository: MonitoredPartialEmailAddressRepository,
) {

  @Operation(summary = "List all monitored email keywords (SEC-113)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS', 'FIU')")
  @GetMapping("/")
  fun listKeywords(
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<MonitoredEmailDto> {
    val results = repository.findAllByOrderByKeywordAsc().map { MonitoredEmailDto(it.keyword) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Create a monitored email keyword (SEC-110 to SEC-112)")
  @PreAuthorize("hasRole('FIU')")
  @PostMapping("/")
  fun createKeyword(@RequestBody request: MonitoredEmailDto): ResponseEntity<MonitoredEmailDto> {
    val keyword = request.keyword.lowercase().trim()
    if (keyword.length < 3) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword must be at least 3 characters")
    }
    if (repository.existsByKeywordIgnoreCase(keyword)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword already exists")
    }
    val saved = repository.save(MonitoredPartialEmailAddress(keyword = keyword))
    return ResponseEntity.status(HttpStatus.CREATED).body(MonitoredEmailDto(saved.keyword))
  }

  @Operation(summary = "Delete a monitored email keyword (SEC-117)")
  @PreAuthorize("hasRole('FIU')")
  @DeleteMapping("/{keyword}/")
  fun deleteKeyword(@PathVariable keyword: String): ResponseEntity<Void> {
    val entity = repository.findByKeywordIgnoreCase(keyword.lowercase())
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "keyword not found")
    repository.delete(entity)
    return ResponseEntity.noContent().build()
  }
}
