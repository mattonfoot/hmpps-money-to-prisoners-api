package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_SECURITY
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.AcceptCheckRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PatchCheckRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.RejectCheckRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SecurityCheckDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.SecurityCheckService
import java.security.Principal
import java.time.LocalDateTime

@RestController
@RequestMapping("/security/checks", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = TAG_SECURITY)
class SecurityCheckResource(
  private val securityCheckService: SecurityCheckService,
) {

  @Operation(summary = "List security checks with optional filtering")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/")
  fun listChecks(
    @RequestParam("status") status: CheckStatus? = null,
    @RequestParam("rules") rules: String? = null,
    @RequestParam("started_at__gte")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    startedAtGte: LocalDateTime? = null,
    @RequestParam("started_at__lt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    startedAtLt: LocalDateTime? = null,
    @RequestParam("actioned_by__isnull") actionedByIsNull: Boolean? = null,
    @RequestParam("credit_resolution") creditResolution: String? = null,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<SecurityCheckDto> {
    val checks = securityCheckService.listChecks(
      status = status,
      rules = rules,
      startedAtGte = startedAtGte,
      startedAtLt = startedAtLt,
      actionedByIsNull = actionedByIsNull,
      creditResolution = creditResolution,
    )
    val results = checks.map { SecurityCheckDto.from(it) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Get a single security check by ID")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{id}/")
  fun getCheck(@PathVariable id: Long): SecurityCheckDto = SecurityCheckDto.from(securityCheckService.getCheck(id))

  @Operation(summary = "Assign a security check to a user")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @RequestMapping(value = ["/{id}/"], method = [RequestMethod.PATCH, RequestMethod.PUT])
  fun patchCheck(
    @PathVariable id: Long,
    @RequestBody request: PatchCheckRequest,
  ): SecurityCheckDto = SecurityCheckDto.from(securityCheckService.patchCheck(id, request.assignedTo))

  @Operation(summary = "Accept a security check (SEC-020 to SEC-025)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @PostMapping("/{id}/accept/")
  fun acceptCheck(
    @PathVariable id: Long,
    @Valid @RequestBody request: AcceptCheckRequest,
    principal: Principal,
  ): ResponseEntity<Void> {
    securityCheckService.acceptCheck(id, principal.name, request.decisionReason)
    return ResponseEntity.noContent().build()
  }

  @Operation(summary = "Reject a security check (SEC-026 to SEC-030)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @PostMapping("/{id}/reject/")
  fun rejectCheck(
    @PathVariable id: Long,
    @Valid @RequestBody request: RejectCheckRequest,
    principal: Principal,
  ): ResponseEntity<Void> {
    securityCheckService.rejectCheck(id, principal.name, request.decisionReason, request.rejectionReasons)
    return ResponseEntity.noContent().build()
  }
}
