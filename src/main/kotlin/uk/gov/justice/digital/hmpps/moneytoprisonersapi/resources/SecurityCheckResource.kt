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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.AcceptCheckRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.RejectCheckRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SecurityCheckDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.SecurityCheckService
import java.security.Principal
import java.time.LocalDateTime

@RestController
@RequestMapping("/security/checks", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Security Checks", description = "Endpoints for managing security checks on credits")
class SecurityCheckResource(
  private val securityCheckService: SecurityCheckService,
) {

  @Operation(summary = "List security checks with optional filtering")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
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
  ): PaginatedResponse<SecurityCheckDto> {
    val checks = securityCheckService.listChecks(
      status = status,
      rules = rules,
      startedAtGte = startedAtGte,
      startedAtLt = startedAtLt,
    )
    val results = checks.map { SecurityCheckDto.from(it) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(summary = "Accept a security check (SEC-020 to SEC-025)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
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
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
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
