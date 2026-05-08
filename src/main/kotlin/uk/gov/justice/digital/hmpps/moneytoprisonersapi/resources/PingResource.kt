package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// Mirrors Django's `/ping.json` so the same compatibility test suite can hit
// either API target without branching on the health-check path.
@RestController
class PingResource {
  @GetMapping("/ping.json", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun ping(): Map<String, String> = mapOf("*" to "ok")
}
