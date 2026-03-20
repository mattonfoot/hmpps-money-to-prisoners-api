package uk.gov.justice.digital.hmpps.moneytoprisonersapi.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

@DisplayName("ClientIdPermissions")
class ClientIdPermissionsTest {

  private fun makeJwt(clientId: String?): Jwt {
    val claims = mutableMapOf<String, Any>("sub" to "user1")
    if (clientId != null) claims["client_id"] = clientId
    return Jwt(
      "token-value",
      Instant.now(),
      Instant.now().plusSeconds(3600),
      mapOf("alg" to "RS256"),
      claims,
    )
  }

  @Nested
  @DisplayName("COR-021: NomsOps client ID permission")
  inner class NomsOpsPermission {

    @Test
    fun `COR-021 allows noms-ops client`() {
      val auth = JwtAuthenticationToken(makeJwt("noms-ops"))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.NOMS_OPS)).isTrue()
    }

    @Test
    fun `COR-021 denies non noms-ops client`() {
      val auth = JwtAuthenticationToken(makeJwt("cashbook"))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.NOMS_OPS)).isFalse()
    }
  }

  @Nested
  @DisplayName("COR-022: SendMoney client ID permission")
  inner class SendMoneyPermission {

    @Test
    fun `COR-022 allows send-money client`() {
      val auth = JwtAuthenticationToken(makeJwt("send-money"))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.SEND_MONEY)).isTrue()
    }

    @Test
    fun `COR-022 denies non send-money client`() {
      val auth = JwtAuthenticationToken(makeJwt("noms-ops"))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.SEND_MONEY)).isFalse()
    }
  }

  @Nested
  @DisplayName("COR-023: BankAdmin client ID permission")
  inner class BankAdminPermission {

    @Test
    fun `COR-023 allows bank-admin client`() {
      val auth = JwtAuthenticationToken(makeJwt("bank-admin"))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.BANK_ADMIN)).isTrue()
    }

    @Test
    fun `COR-023 denies non bank-admin client`() {
      val auth = JwtAuthenticationToken(makeJwt("send-money"))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.BANK_ADMIN)).isFalse()
    }
  }

  @Nested
  @DisplayName("COR-024: Cashbook client ID permission")
  inner class CashbookPermission {

    @Test
    fun `COR-024 allows cashbook client`() {
      val auth = JwtAuthenticationToken(makeJwt("cashbook"))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.CASHBOOK)).isTrue()
    }

    @Test
    fun `COR-024 denies non cashbook client`() {
      val auth = JwtAuthenticationToken(makeJwt("bank-admin"))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.CASHBOOK)).isFalse()
    }
  }

  @Nested
  @DisplayName("Edge cases")
  inner class EdgeCases {

    @Test
    fun `returns false when authentication is not a JWT token`() {
      val auth = TestingAuthenticationToken("user", "password")
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.NOMS_OPS)).isFalse()
    }

    @Test
    fun `returns false when JWT has no client_id claim`() {
      val auth = JwtAuthenticationToken(makeJwt(null))
      assertThat(ClientIdPermissions.isAllowed(auth, ClientApplication.NOMS_OPS)).isFalse()
    }
  }
}
