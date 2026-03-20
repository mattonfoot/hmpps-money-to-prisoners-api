package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ServiceStatusDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.ServiceAvailabilityService

@RestController
@RequestMapping(produces = ["application/json"])
@Tag(name = "Service Availability", description = "Public endpoint exposing service up/down status")
class ServiceAvailabilityResource(
  private val serviceAvailabilityService: ServiceAvailabilityService,
) {

  // -------------------------------------------------------------------------
  // SVC-001 to SVC-005: GET /service-availability/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Get service availability",
    description = "Returns the availability status for each known service plus a wildcard '*' entry " +
      "that is true only when all services are up. " +
      "When a service is down, the response includes downtime_end (if a recovery time is set) and " +
      "message_to_users (if a message is set). " +
      "This endpoint is public — no authentication required (SVC-001 to SVC-005).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Map of service name to availability status",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ServiceAvailabilityResponse::class))],
      ),
    ],
  )
  @SecurityRequirements
  @PreAuthorize("permitAll()")
  @GetMapping("/service-availability/")
  fun getServiceAvailability(): Map<String, ServiceStatusDto> = serviceAvailabilityService.getServiceAvailability()
}

@Schema(name = "ServiceAvailabilityResponse", description = "Map of service name to status. Keys are service identifiers and '*'.")
private class ServiceAvailabilityResponse(
  @Schema(description = "Status for GOV.UK Pay")
  val gov_uk_pay: ServiceStatusDto,
  @Schema(description = "Wildcard status — true only if all services are up")
  val `*`: ServiceStatusDto,
)
