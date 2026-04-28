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
@RequestMapping("/recipients", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Recipient Profiles", description = "Recipient profile management and monitoring (SEC-100 to SEC-110)")
class RecipientProfileResource(
  private val recipientProfileService: RecipientProfileService,
  private val disbursementRepository: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository,
) {

  @Operation(summary = "List recipient profiles (SEC-100 to SEC-108)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/")
  fun listProfiles(
    @RequestParam("monitoring") monitoring: Boolean? = null,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
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
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Get a single recipient profile by ID")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{id}/")
  fun getProfile(@PathVariable id: Long, principal: Principal): RecipientProfileDto {
    val profile = recipientProfileService.getProfile(id)
    return RecipientProfileDto.from(profile, currentUsername = principal.name)
  }

  @Operation(summary = "Get disbursements for a recipient profile")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{recipient_pk}/disbursements/")
  fun listDisbursements(
    @PathVariable("recipient_pk") id: Long,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<DisbursementDto> {
    val disbursements = recipientProfileService.getDisbursements(id)
    val results = disbursements.map { DisbursementDto.from(it) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Get a single disbursement for a recipient profile")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{recipient_pk}/disbursements/{id}/")
  fun getDisbursementForRecipient(
    @PathVariable("recipient_pk") recipientPk: Long,
    @PathVariable id: Long,
  ): ResponseEntity<DisbursementDto> {
    val disbursement = disbursementRepository.findById(id).orElse(null)
      ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(DisbursementDto.from(disbursement))
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
