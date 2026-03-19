package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/security/recipients", produces = ["application/json"])
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Recipient Profiles", description = "Stub recipient profile monitoring endpoints (SEC-100 to SEC-105)")
class RecipientProfileResource {

  @Operation(summary = "Monitor a recipient profile (stub)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/monitor/")
  fun monitor(@PathVariable id: Long): ResponseEntity<Void> = ResponseEntity.noContent().build()

  @Operation(summary = "Unmonitor a recipient profile (stub)")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/unmonitor/")
  fun unmonitor(@PathVariable id: Long): ResponseEntity<Void> = ResponseEntity.noContent().build()
}
