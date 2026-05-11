package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_NOTIFICATIONS
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.EmailPreferencesDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.Event
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.EventPagesResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.Notification
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.Rule
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SetEmailPreferencesRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailFrequency
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.ENABLED_RULE_CODES
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.NotificationService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.RULES
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.ServiceNotificationService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.security.Principal
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = ["application/json"])
@SecurityRequirement(name = "oauth2_provider")
@Tag(name = TAG_NOTIFICATIONS)
class NotificationsResource(
  private val notificationService: NotificationService,
  private val serviceNotificationService: ServiceNotificationService,
) {

  // -------------------------------------------------------------------------
  // NOT-003 to NOT-006: GET /events/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List notification events",
    description = "Returns events visible to the current user — their own events plus global (user-agnostic) events. " +
      "Supports filtering by rule code (multiple values) and triggered_at date range. " +
      "Ordered by triggered_at descending, then id ascending (NOT-003 to NOT-006).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of notification events",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = EventPaginatedResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/events/")
  fun listEvents(
    @Parameter(description = "Filter by rule code (repeatable, e.g. rule=MONP&rule=MONS)")
    @RequestParam("rule")
    rules: List<String>? = null,
    @Parameter(description = "Filter events triggered on or after this datetime (inclusive)", example = "2024-01-01T00:00:00")
    @RequestParam("triggered_at__gte")
    triggeredAtGte: OffsetDateTime? = null,
    @Parameter(description = "Filter events triggered before this datetime (exclusive)", example = "2024-02-01T00:00:00")
    @RequestParam("triggered_at__lt")
    triggeredAtLt: OffsetDateTime? = null,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
    principal: Principal,
  ): PaginatedResponse<Event> {
    val events = notificationService.listEvents(
      username = principal.name,
      rules = rules,
      triggeredAtGte = triggeredAtGte,
      triggeredAtLt = triggeredAtLt,
    )
    val results = events.map { Event.from(it) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  // -------------------------------------------------------------------------
  // NOT-007: GET /events/pages/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Get event date pagination info",
    description = "Returns the oldest and newest dates that have notification events for the current user, " +
      "along with the total count of distinct dates. Used by noms-ops to drive date-based page navigation (NOT-007).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Date pagination summary",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = EventPagesResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/events/pages/")
  fun getEventPages(
    @Parameter(description = "Filter by rule code (repeatable)")
    @RequestParam("rule")
    rules: List<String>? = null,
    @Parameter(description = "Number of distinct dates to skip", example = "0")
    @RequestParam("offset", defaultValue = "0")
    offset: Int = 0,
    @Parameter(description = "Maximum number of distinct dates to return", example = "25")
    @RequestParam("limit", defaultValue = "25")
    limit: Int = 25,
    principal: Principal,
  ): EventPagesResponse {
    val (newest, oldest, count) = notificationService.getEventPages(
      username = principal.name,
      rules = rules,
      offset = offset,
      limit = limit,
    )
    return EventPagesResponse(
      newest = newest?.let { java.time.LocalDate.parse(it) },
      oldest = oldest?.let { java.time.LocalDate.parse(it) },
      count = count,
    )
  }

  // -------------------------------------------------------------------------
  // NOT-008: GET /rules/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "List enabled notification rules",
    description = "Returns the set of enabled notification rules with their codes and descriptions (NOT-008).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Paginated list of enabled rules",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = RulePaginatedResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/rules/")
  fun listRules(
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<Rule> {
    val rules = RULES.values
      .filter { it.code in ENABLED_RULE_CODES }
      .map { Rule(code = it.code, description = it.description) }
    return PaginatedResponse.fromList(rules, limit = limit, offset = offset)
  }

  // -------------------------------------------------------------------------
  // NOT-010 to NOT-012: GET and POST /emailpreferences/
  // -------------------------------------------------------------------------

  @Operation(
    summary = "Get email notification preferences",
    description = "Returns the current user's email notification frequency. Defaults to 'never' if no preference has been set (NOT-010).",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Email frequency preference",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = EmailPreferencesDto::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/emailpreferences/")
  fun getEmailPreferences(principal: Principal): EmailPreferencesDto {
    val frequency = notificationService.getEmailFrequency(principal.name)
    return EmailPreferencesDto(frequency = frequency)
  }

  @Operation(
    summary = "Set email notification preferences",
    description = "Sets the current user's email notification frequency. Creates or updates the preference record (NOT-011, NOT-012).",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Preference saved successfully"),
      ApiResponse(
        responseCode = "400",
        description = "Invalid or missing frequency value",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/emailpreferences/")
  fun setEmailPreferences(
    @RequestBody request: SetEmailPreferencesRequest,
    principal: Principal,
  ): ResponseEntity<Any> {
    val frequency = try {
      request.frequency?.let { EmailFrequency.fromValue(it) }
    } catch (_: IllegalArgumentException) {
      null
    } ?: return ResponseEntity.badRequest()
      .body(mapOf("frequency" to listOf("Must provide a recognized \"frequency\" value")))
    notificationService.setEmailFrequency(principal.name, frequency)
    return ResponseEntity.noContent().build()
  }

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
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
    authentication: Authentication?,
  ): PaginatedResponse<Notification> {
    val authenticated = authentication != null &&
      authentication.isAuthenticated &&
      authentication !is AnonymousAuthenticationToken
    val notifications = serviceNotificationService.listNotifications(authenticated, targetPrefix)
    return PaginatedResponse.fromList(notifications, limit = limit, offset = offset)
  }
}

@Schema(name = "PaginatedResponseNotification", description = "Paginated response containing service notifications")
private class ServiceNotificationPaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of active notifications")
  val results: List<Notification>,
)

@Schema(name = "PaginatedResponseEvent", description = "Paginated response containing notification events")
private class EventPaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of events")
  val results: List<Event>,
)

@Schema(name = "PaginatedResponseRule", description = "Paginated response containing notification rules")
private class RulePaginatedResponse(
  @Schema(description = "Total number of results")
  val count: Int,
  @Schema(description = "URL of the next page, or null", nullable = true)
  val next: String?,
  @Schema(description = "URL of the previous page, or null", nullable = true)
  val previous: String?,
  @Schema(description = "List of rules")
  val results: List<Rule>,
)
