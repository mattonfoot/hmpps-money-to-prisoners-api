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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.RecipientProfileDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.RecipientProfileService
import java.security.Principal

@RestController
@RequestMapping("/security/recipients", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Recipient Profiles", description = "Recipient profile management and monitoring (SEC-100 to SEC-110)")
class RecipientProfileResource(
  private val recipientProfileService: RecipientProfileService,
) {

  @Operation(summary = "List recipient profiles (SEC-100 to SEC-108)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @GetMapping("/")
  fun listProfiles(
    @RequestParam("monitoring") monitoring: Boolean? = null,
    principal: Principal,
  ): PaginatedResponse<RecipientProfileDto> {
    val (monitoredBy, notMonitoredBy) = when (monitoring) {
      true -> principal.name to null
      false -> null to principal.name
      null -> null to null
    }
    val profiles = recipientProfileService.listProfiles(
      monitoredByUsername = monitoredBy,
      notMonitoredByUsername = notMonitoredBy,
    )
    val results = profiles.map { RecipientProfileDto.from(it, currentUsername = principal.name) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(summary = "Get a single recipient profile by ID")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @GetMapping("/{id}/")
  fun getProfile(@PathVariable id: Long, principal: Principal): RecipientProfileDto {
    val profile = recipientProfileService.getProfile(id)
    return RecipientProfileDto.from(profile, currentUsername = principal.name)
  }

  @Operation(summary = "Get disbursements for a recipient profile")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @GetMapping("/{id}/disbursements/")
  fun listDisbursements(@PathVariable id: Long): PaginatedResponse<DisbursementDto> {
    val disbursements = recipientProfileService.getDisbursements(id)
    val results = disbursements.map { DisbursementDto.from(it) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(summary = "Monitor a recipient profile (SEC-105)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/monitor/")
  fun monitor(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
    recipientProfileService.monitor(id, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(summary = "Unmonitor a recipient profile (SEC-106)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/unmonitor/")
  fun unmonitor(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
    recipientProfileService.unmonitor(id, principal.name)
    return ResponseEntity.noContent().build()
  }
}
