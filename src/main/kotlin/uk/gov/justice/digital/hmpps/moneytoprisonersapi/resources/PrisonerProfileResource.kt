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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.config.TAG_PRISONERS
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PrisonerProfileDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.SecurityCreditDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.PrisonerProfileService
import java.security.Principal

@RestController
@RequestMapping("/prisoners", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = TAG_PRISONERS)
class PrisonerProfileResource(
  private val prisonerProfileService: PrisonerProfileService,
  private val disbursementRepository: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository,
  private val prisonRepository: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository,
  private val prisonerProfileRepository: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository,
) {

  private fun prisonNameMap(): Map<String, String> = prisonRepository.findAll().associate { it.nomisId to it.name }

  @Operation(summary = "List prisoner profiles (SEC-090 to SEC-098)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/")
  fun listProfiles(
    @RequestParam("monitoring") monitoring: Boolean? = null,
    @RequestParam("simple_search") simpleSearch: String? = null,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
    principal: Principal,
  ): PaginatedResponse<PrisonerProfileDto> {
    val (monitoredBy, notMonitoredBy) = when (monitoring) {
      true -> principal.name to null
      false -> null to principal.name
      null -> null to null
    }
    val profiles = prisonerProfileService.listProfiles(
      monitoredByUsername = monitoredBy,
      notMonitoredByUsername = notMonitoredBy,
      simpleSearch = simpleSearch,
    )
    val results = profiles.map { buildDto(it, principal.name) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Get a single prisoner profile by ID")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{id}/")
  fun getProfile(@PathVariable id: Long, principal: Principal): PrisonerProfileDto {
    val profile = prisonerProfileService.getProfile(id)
    return buildDto(profile, principal.name)
  }

  private fun buildDto(profile: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile, username: String): PrisonerProfileDto {
    val disbursements = disbursementRepository.findByPrisonerNumber(profile.prisonerNumber ?: "")
    val senderCount = profile.id?.let { prisonerProfileRepository.countSendersForProfile(it) } ?: 0
    // recipientCount = distinct recipients this prisoner has sent disbursements to
    val recipientCount = disbursements.mapNotNull {
      val sc = it.sortCode
      val an = it.accountNumber
      if (sc != null && an != null) "$sc-$an" else null
    }.distinct().size
    return PrisonerProfileDto.from(
      profile = profile,
      currentUsername = username,
      disbursements = disbursements,
      senderCount = senderCount,
      recipientCount = recipientCount,
    )
  }

  @Operation(summary = "Get credits for a prisoner profile (SEC-093)")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{prisoner_pk}/credits/")
  fun listCredits(
    @PathVariable("prisoner_pk") id: Long,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<SecurityCreditDto> {
    val profile = prisonerProfileService.getProfile(id)
    val results = profile.credits.map { SecurityCreditDto.from(it, prisonerProfileId = profile.id) }
    return PaginatedResponse.fromList(results, limit = limit, offset = offset)
  }

  @Operation(summary = "Get disbursements for a prisoner profile")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{prisoner_pk}/disbursements/")
  fun listDisbursements(
    @PathVariable("prisoner_pk") id: Long,
    @RequestParam("limit", defaultValue = "20") limit: Int = 20,
    @RequestParam("offset", defaultValue = "0") offset: Int = 0,
  ): PaginatedResponse<uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementDto> {
    val profile = prisonerProfileService.getProfile(id)
    val disbursements = disbursementRepository.findByPrisonerNumber(profile.prisonerNumber ?: "")
      .map { uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementDto.from(it, prisonNameMap()) }
    return PaginatedResponse.fromList(disbursements, limit = limit, offset = offset)
  }

  @Operation(summary = "Get a single disbursement for a prisoner profile")
  @PreAuthorize("hasAnyRole('SECURITY_STAFF', 'NOMS_OPS')")
  @GetMapping("/{prisoner_pk}/disbursements/{id}/")
  fun getDisbursementForPrisoner(
    @PathVariable("prisoner_pk") _prisonerPk: Long,
    @PathVariable id: Long,
  ): ResponseEntity<uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementDto> {
    val disbursement = disbursementRepository.findById(id).orElse(null)
      ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementDto.from(disbursement, prisonNameMap()))
  }

  @Operation(summary = "Monitor a prisoner profile (SEC-062)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/monitor/")
  fun monitor(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
    prisonerProfileService.monitor(id, principal.name)
    return ResponseEntity.noContent().build()
  }

  @Operation(summary = "Unmonitor a prisoner profile (SEC-063)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/unmonitor/")
  fun unmonitor(@PathVariable id: Long, principal: Principal): ResponseEntity<Void> {
    prisonerProfileService.unmonitor(id, principal.name)
    return ResponseEntity.noContent().build()
  }
}
