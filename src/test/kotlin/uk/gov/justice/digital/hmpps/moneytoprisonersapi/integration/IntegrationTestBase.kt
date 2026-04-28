package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.ContainersConfig
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration.helpers.IntegrationTestHelpers

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Import(IntegrationTestHelpers::class, ContainersConfig::class)
abstract class IntegrationTestBase {

  @LocalServerPort
  private var port: Int = 0

  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var integrationTestHelpers: IntegrationTestHelpers

  @BeforeEach
  fun initClients() {
    webTestClient = WebTestClient.bindToServer()
      .baseUrl("http://localhost:$port")
      .build()
    integrationTestHelpers.setWebClient(webTestClient)
  }

  /**
   * Returns a header-mutating function that sets the Authorization header
   * with a pre-seeded OAuth2 access token from the database.
   *
   * Token selection is based on the requested roles:
   * - ROLE_BANK_ADMIN → test-token-bank-admin
   * - ROLE_PRISON_CLERK → test-token-prison-clerk
   * - ROLE_SECURITY_STAFF → test-token-security
   * - ROLE_SEND_MONEY → test-token-send-money
   * - ROLE_FIU → test-token-fiu
   * - ROLE_NOMS_OPS → test-token-prisoner-location-admin
   * - No specific roles → test-token-admin (superuser with all groups)
   */
  fun setAuthorisation(
    username: String? = null,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit {
    val token = resolveToken(roles)
    return { headers -> headers.setBearerAuth(token) }
  }

  private fun resolveToken(roles: List<String>): String {
    if (roles.isEmpty()) return "test-token-no-roles"

    return when {
      roles.any { it.contains("DISBURSEMENT_BANK_ADMIN", ignoreCase = true) } -> "test-token-disbursement-admin"
      roles.any { it.contains("BANK_ADMIN", ignoreCase = true) } -> "test-token-bank-admin"
      roles.any { it.contains("PRISON_CLERK", ignoreCase = true) } -> "test-token-prison-clerk"
      roles.any { it.contains("SECURITY_STAFF", ignoreCase = true) || it.contains("SECURITY", ignoreCase = true) } -> "test-token-security"
      roles.any { it.contains("SEND_MONEY", ignoreCase = true) } -> "test-token-send-money"
      roles.any { it.contains("FIU", ignoreCase = true) } -> "test-token-fiu"
      roles.any { it.contains("NOMS_OPS", ignoreCase = true) } -> "test-token-prisoner-location-admin"
      roles.any { it.contains("USER_ADMIN", ignoreCase = true) } -> "test-token-fiu"
      else -> "test-token-admin"
    }
  }
}
