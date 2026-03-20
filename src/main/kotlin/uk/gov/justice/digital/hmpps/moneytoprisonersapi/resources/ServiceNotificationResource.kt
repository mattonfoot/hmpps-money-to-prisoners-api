package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.ServiceNotificationDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.ServiceNotificationService

@RestController
@RequestMapping(produces = ["application/json"])
@Tag(name = "Service Notifications", description = "Banner and alert notifications shown in front-end applications")
class ServiceNotificationResource(
  private val serviceNotificationService: ServiceNotificationService,
) {

  // -------------------------------------------------------------------------
  // SVC-010 to SVC-014: GET /notifications/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List active service notifications",
    description = "Returns active banner/alert notifications. " +
      "Unauthenticated users only see notifications marked public=true (SVC-011). " +
      "Supports filtering by target prefix via target__startswith (SVC-012). " +
      "Active = now is between start and end (SVC-014). " +
      "This endpoint is public — no authentication required.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of active notifications",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ServiceNotificationPaginatedResponse::class))],
      ),
    ],
  )
  @SecurityRequirements
  @PreAuthorize("permitAll()")
  @GetMapping("/notifications/")
  fun listNotifications(
    @Parameter(description = "Filter notifications whose target starts with this prefix (e.g. cashbook, noms_ops)")
    @RequestParam("target__startswith")
    targetPrefix: String?,
    authentication: Authentication?,
  ): PaginatedResponse<ServiceNotificationDto> {
    val authenticated = authentication != null &&
      authentication.isAuthenticated &&
      authentication !is AnonymousAuthenticationToken
    val notifications = serviceNotificationService.listNotifications(authenticated, targetPrefix)
    return PaginatedResponse(count = notifications.size, results = notifications)
  }
}

@Schema(name = "PaginatedResponseServiceNotificationDto", description = "Paginated response containing service notifications")
private class ServiceNotificationPaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of active notifications")
  val results: List<ServiceNotificationDto>,
)
