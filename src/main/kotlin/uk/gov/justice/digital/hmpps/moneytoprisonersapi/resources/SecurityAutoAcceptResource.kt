package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.AutoAcceptRuleDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateAutoAcceptRuleRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PatchAutoAcceptRuleRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.SecurityCheckService
import java.security.Principal

@RestController
@RequestMapping("/security/checks/auto-accept", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Security Auto-Accept Rules", description = "Auto-accept rules for security checks")
class SecurityAutoAcceptResource(
  private val securityCheckService: SecurityCheckService,
) {

  @Operation(summary = "List auto-accept rules (SEC-040 to SEC-047)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @GetMapping("/")
  fun listRules(
    @RequestParam("is_active") isActive: Boolean? = null,
    @RequestParam("sender_profile") senderProfileId: Long? = null,
    @RequestParam("prisoner_profile") prisonerProfileId: Long? = null,
  ): PaginatedResponse<AutoAcceptRuleDto> {
    val rules = securityCheckService.listAutoAcceptRules(
      isActive = isActive,
      senderProfileId = senderProfileId,
      prisonerProfileId = prisonerProfileId,
    )
    val results = rules.map { AutoAcceptRuleDto.from(it) }
    return PaginatedResponse(count = results.size, results = results)
  }

  @Operation(summary = "Create an auto-accept rule (SEC-041)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @PostMapping("/")
  fun createRule(
    @RequestBody request: CreateAutoAcceptRuleRequest,
    principal: Principal,
  ): ResponseEntity<AutoAcceptRuleDto> {
    if (request.states.size != 1) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "states must contain exactly 1 element")
    }
    val stateReq = request.states.first()
    val rule = securityCheckService.createAutoAcceptRule(
      senderProfileId = request.senderProfile,
      prisonerProfileId = request.prisonerProfile,
      initialState = {},
      createdBy = principal.name,
      active = stateReq.active,
      reason = stateReq.reason,
    )
    return ResponseEntity.status(HttpStatus.CREATED).body(AutoAcceptRuleDto.from(rule))
  }

  @Operation(summary = "Patch an auto-accept rule – append a new state (SEC-044)")
  @PreAuthorize("hasAnyRole('ROLE_SECURITY_STAFF', 'ROLE_NOMS_OPS')")
  @PatchMapping("/{id}/")
  fun patchRule(
    @PathVariable id: Long,
    @RequestBody request: PatchAutoAcceptRuleRequest,
    principal: Principal,
  ): ResponseEntity<AutoAcceptRuleDto> {
    if (request.states.isEmpty()) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "states must not be empty")
    }
    val stateReq = request.states.first()
    val rule = securityCheckService.patchAutoAcceptRule(
      id = id,
      active = stateReq.active,
      reason = stateReq.reason,
      createdBy = principal.name,
    )
    return ResponseEntity.ok(AutoAcceptRuleDto.from(rule))
  }
}
