package uk.gov.justice.digital.hmpps.moneytoprisonersapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
class ResourceServerConfiguration {

  /**
   * Allow public (unauthenticated) access to GET /prisons/.
   * All other security configuration is provided by the HMPPS Kotlin Spring Boot starter.
   */
  @Bean
  fun resourceServerConfigurationCustomizer(): ResourceServerConfigurationCustomizer = ResourceServerConfigurationCustomizer {
    unauthorizedRequestPaths {
      addPaths = setOf("/prisons/")
    }
  }
}
