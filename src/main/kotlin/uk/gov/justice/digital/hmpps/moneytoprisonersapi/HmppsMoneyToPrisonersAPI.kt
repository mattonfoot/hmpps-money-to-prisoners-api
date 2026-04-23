package uk.gov.justice.digital.hmpps.moneytoprisonersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import uk.gov.justice.hmpps.kotlin.auth.HmppsResourceServerConfiguration
import uk.gov.justice.hmpps.kotlin.clienttracking.HmppsClientTrackingConfiguration

@SpringBootApplication(
  exclude = [
    HmppsResourceServerConfiguration::class,
    HmppsClientTrackingConfiguration::class,
  ],
)
class HmppsMoneyToPrisonersAPI

fun main(args: Array<String>) {
  runApplication<HmppsMoneyToPrisonersAPI>(*args)
}
