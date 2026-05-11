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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_SENDERS
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SecurityCredit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.SenderProfileService
import java.security.Principal

@RestController
@RequestMapping("/senders", produces = ["application/json"])
@SecurityRequirement(name = "oauth2_provider")
@Tag(name = TAG_SENDERS)
class SendersResource(
  private val senderProfileService: SenderProfileService,
) {

  @Operation(summary = "List sender profiles (SEC-070 to SEC-080)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/")
  fun listProfiles(
    @RequestParam("monitoring") monitoring: Boolean? = null,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
    principal: Principal,
  ): PaginatedResponse<SenderProfile> {
    val (monitoredBy, notMonitoredBy) = when (monitoring) {
      true -> principal.name to null
      false -> null to principal.name
      null -> null to null
    }
    val profiles = senderProfileService.listProfiles(monitoredByUsername = monitoredBy, notMonitoredByUsername = notMonitoredBy)
    val results = profiles.map {
      SenderProfile.from(
        it,
        currentUsername = principal.name,
        isMonitoredByCurrentUser = senderProfileService.isMonitoredBy(it.id!!, principal.name),
      )
    }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Get a single sender profile by ID")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{id}/")
  fun getProfile(@PathVariable id: Long, principal: Principal): SenderProfile {
    val profile = senderProfileService.getProfile(id)
    return SenderProfile.from(
      profile,
      currentUsername = principal.name,
      isMonitoredByCurrentUser = senderProfileService.isMonitoredBy(profile.id!!, principal.name),
    )
  }

  @Operation(summary = "Get credits for a sender profile (SEC-075)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{sender_pk}/credits/")
  fun listCredits(
    @PathVariable("sender_pk") id: Long,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<SecurityCredit> {
    val profile = senderProfileService.getProfile(id)
    val results = profile.credits.map { SecurityCredit.from(it, senderProfileId = profile.id) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
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
