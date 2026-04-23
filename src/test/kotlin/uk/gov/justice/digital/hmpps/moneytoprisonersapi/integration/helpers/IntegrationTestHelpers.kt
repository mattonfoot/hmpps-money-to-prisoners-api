package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration.helpers

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient

@TestConfiguration
class IntegrationTestHelpers {

  lateinit var webTestClient: WebTestClient

  fun setWebClient(webClient: WebTestClient) {
    webTestClient = webClient
  }

  internal fun setAuthorisation(
    username: String? = null,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit {
    val token = when {
      roles.any { it.contains("BANK_ADMIN", ignoreCase = true) } -> "test-token-bank-admin"
      roles.any { it.contains("PRISON_CLERK", ignoreCase = true) } -> "test-token-prison-clerk"
      roles.any { it.contains("SECURITY", ignoreCase = true) } -> "test-token-security"
      roles.any { it.contains("SEND_MONEY", ignoreCase = true) } -> "test-token-send-money"
      else -> "test-token-admin"
    }
    return { headers -> headers.setBearerAuth(token) }
  }
}
