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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SecurityCreditDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonerProfileService
import java.security.Principal

@RestController
@RequestMapping("/security/prisoners", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Prisoner Profiles", description = "Prisoner profile management and monitoring")
class PrisonerProfileResource(
  private val prisonerProfileService: PrisonerProfileService,
) {

  @Operation(summary = "List prisoner profiles (SEC-090 to SEC-098)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @GetMapping("/")
  fun listProfiles(): PaginatedResponse<PrisonerProfileDto> {
    val profiles = prisonerProfileService.listProfiles()
    val results = profiles.map { PrisonerProfileDto.from(it) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(summary = "Get credits for a prisoner profile (SEC-093)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @GetMapping("/{id}/credits/")
  fun listCredits(@PathVariable id: Long): PaginatedResponse<SecurityCreditDto> {
    val profile = prisonerProfileService.getProfile(id)
    val results = profile.credits.map { SecurityCreditDto.from(it, prisonerProfileId = profile.id) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(summary = "Monitor a prisoner profile (SEC-062)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/monitor/")
  fun monitor(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
    prisonerProfileService.monitor(id, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(summary = "Unmonitor a prisoner profile (SEC-063)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/unmonitor/")
  fun unmonitor(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
    prisonerProfileService.unmonitor(id, principal.name)
    return ResponseEntity.noContent().build()
  }
}
