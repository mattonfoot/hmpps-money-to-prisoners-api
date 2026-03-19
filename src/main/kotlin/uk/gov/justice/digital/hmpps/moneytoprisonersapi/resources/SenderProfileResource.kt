package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SecurityCreditDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SenderProfileDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.SenderProfileService
import java.security.Principal

@RestController
@RequestMapping("/security/senders", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Sender Profiles", description = "Sender profile management and monitoring")
class SenderProfileResource(
  private val senderProfileService: SenderProfileService,
) {

  @Operation(summary = "List sender profiles (SEC-070 to SEC-080)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @GetMapping("/")
  fun listProfiles(): PaginatedResponse<SenderProfileDto> {
    val profiles = senderProfileService.listProfiles()
    val results = profiles.map { SenderProfileDto.from(it) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(summary = "Get credits for a sender profile (SEC-075)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @GetMapping("/{id}/credits/")
  fun listCredits(@PathVariable id: Long): PaginatedResponse<SecurityCreditDto> {
    val profile = senderProfileService.getProfile(id)
    val results = profile.credits.map { SecurityCreditDto.from(it, senderProfileId = profile.id) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(summary = "Monitor a sender profile (SEC-060)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/monitor/")
  fun monitor(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
    senderProfileService.monitor(id, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(summary = "Unmonitor a sender profile (SEC-061)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/unmonitor/")
  fun unmonitor(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
    senderProfileService.unmonitor(id, principal.name)
    return ResponseEntity.noContent().build()
  }
}
