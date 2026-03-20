package uk.gov.justice.digital.hmpps.moneytoprisonersapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.security.ClientIdPermissions

/**
 * Registers [ClientIdPermissions] as a Spring bean named "clientIdPermissions"
 * so it can be referenced in @PreAuthorize SpEL expressions, e.g.:
 *   @PreAuthorize("@clientIdPermissions.isBankAdmin(authentication)")
 */
@Configuration
class SecurityPermissionsConfiguration {

  @Bean
  fun clientIdPermissions(): ClientIdPermissions = ClientIdPermissions
}
