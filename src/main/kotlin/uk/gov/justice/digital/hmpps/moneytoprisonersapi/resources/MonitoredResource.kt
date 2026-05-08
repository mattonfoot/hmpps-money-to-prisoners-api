package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_MONITORED
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.MonitoredCountResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.MonitoredEmailDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MonitoredPartialEmailAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MonitoredPartialEmailAddressRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonerProfileService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.SenderProfileService
import java.security.Principal

@RestController
@RequestMapping(produces = ["application/json"])
@SecurityRequirement(name = "oauth2_provider")
@Tag(name = TAG_MONITORED)
class MonitoredResource(
  private val senderProfileService: SenderProfileService,
  private val prisonerProfileService: PrisonerProfileService,
  private val monitoredPartialEmailAddressRepository: MonitoredPartialEmailAddressRepository,
) {

  @Operation(summary = "Get total monitored senders + prisoners for the current user (SEC-067)")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/monitored/")
  fun getMonitoredCount(principal: Principal): MonitoredCountResponse {
    val senderCount = senderProfileService.countMonitoredByUser(principal.name)
    val prisonerCount = prisonerProfileService.countMonitoredByUser(principal.name)
    return MonitoredCountResponse(count = senderCount + prisonerCount)
  }

  @Operation(summary = "List all monitored email keywords (SEC-113)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS', 'FIU')")
  @GetMapping("/security/monitored-email-addresses/")
  fun listKeywords(
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<MonitoredEmailDto> {
    val results = monitoredPartialEmailAddressRepository.findAllByOrderByKeywordAsc().map { MonitoredEmailDto(it.keyword) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Create a monitored email keyword (SEC-110 to SEC-112)")
  @PreAuthorize("hasRole('FIU')")
  @PostMapping("/security/monitored-email-addresses/")
  fun createKeyword(@RequestBody rawBody: String): ResponseEntity<String> {
    // Python's view accepts the keyword as a bare JSON string body, so the
    // payload looks like `"someKeyword"` (with the quotes). Strip them here.
    val keyword = rawBody.trim().removeSurrounding("\"").lowercase()
    if (keyword.length < 3) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword must be at least 3 characters")
    }
    if (monitoredPartialEmailAddressRepository.existsByKeywordIgnoreCase(keyword)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword already exists")
    }
    val saved = monitoredPartialEmailAddressRepository.save(MonitoredPartialEmailAddress().apply { this.keyword = keyword })
    return ResponseEntity.status(HttpStatus.CREATED).body("\"${saved.keyword}\"")
  }

  @Operation(summary = "Delete a monitored email keyword (SEC-117)")
  @PreAuthorize("hasRole('FIU')")
  @DeleteMapping("/security/monitored-email-addresses/{keyword}/")
  fun deleteKeyword(@PathVariable keyword: String): ResponseEntity<Void> {
    val entity = monitoredPartialEmailAddressRepository.findByKeywordIgnoreCase(keyword.lowercase())
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "keyword not found")
    monitoredPartialEmailAddressRepository.delete(entity)
    return ResponseEntity.noContent().build()
  }
}
