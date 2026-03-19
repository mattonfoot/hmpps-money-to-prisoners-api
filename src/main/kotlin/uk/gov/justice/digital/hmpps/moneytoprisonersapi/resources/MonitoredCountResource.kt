package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.MonitoredCountResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonerProfileService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.SenderProfileService
import java.security.Principal

@RestController
@RequestMapping("/monitored", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Monitored Count", description = "Returns the total monitored senders + prisoners for the current user")
class MonitoredCountResource(
  private val senderProfileService: SenderProfileService,
  private val prisonerProfileService: PrisonerProfileService,
) {

  @Operation(summary = "Get total monitored senders + prisoners for the current user (SEC-067)")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/")
  fun getMonitoredCount(principal: Principal): MonitoredCountResponse {
    val senderCount = senderProfileService.countMonitoredByUser(principal.name)
    val prisonerCount = prisonerProfileService.countMonitoredByUser(principal.name)
    return MonitoredCountResponse(count = senderCount + prisonerCount)
  }
}
