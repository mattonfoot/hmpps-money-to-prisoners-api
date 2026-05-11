package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import io.swagger.v3.oas.annotations.security.SecurityRequirements
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// Mirrors Django's `/ping.json` so the same compatibility test suite can hit
// either API target without branching on the health-check path. The endpoint
// is intentionally public — `@SecurityRequirements` (empty) removes the
// inherited global `oauth2_provider` requirement from the OpenAPI doc.
@RestController
class PingResource {
  @SecurityRequirements
  @GetMapping("/ping.json", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun ping(): Map<String, String> = mapOf("*" to "ok")
}
