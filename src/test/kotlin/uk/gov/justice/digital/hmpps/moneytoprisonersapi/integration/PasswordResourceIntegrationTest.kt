package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AuthUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PasswordResetToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.OAuthAccessTokenRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PasswordResetTokenRepository
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordResourceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var oauthAccessTokenRepository: OAuthAccessTokenRepository

  @Autowired
  private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

  @Test
  @DisplayName("AUTH-045 - POST /change_password/ changes password for authenticated user")
  fun `should change password for authenticated user`() {
    val user = userForToken("test-token-prison-clerk").apply {
      password = djangoPassword("current-pass")
    }
    authUserRepository.save(user)

    webTestClient.post()
      .uri("/change_password/")
      .header("Authorization", "Bearer test-token-prison-clerk")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("""{"old_password":"current-pass","new_password":"freshpass"}""")
      .exchange()
      .expectStatus().isNoContent

    val updated = authUserRepository.findById(user.id!!).orElseThrow()
    assertThat(matchesDjangoPassword("freshpass", updated.password)).isTrue()
  }

  @Test
  @DisplayName("AUTH-045 - POST /change_password/ rejects unsupported client application")
  fun `should reject send money client`() {
    webTestClient.post()
      .uri("/change_password/")
      .header("Authorization", "Bearer test-token-send-money")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("""{"old_password":"anything","new_password":"freshpass"}""")
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  @DisplayName("AUTH-045 - POST /change_password/{code}/ updates password without deleting request row")
  fun `should change password with reset code`() {
    val user = userForToken("test-token-prison-clerk").apply {
      password = djangoPassword("current-pass")
    }
    authUserRepository.save(user)
    val code = UUID.randomUUID()
    passwordResetTokenRepository.save(
      PasswordResetToken().apply {
        id = code
        this.user = user
      },
    )

    webTestClient.post()
      .uri("/change_password/$code/")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("""{"new_password":"changed-with-code"}""")
      .exchange()
      .expectStatus().isNoContent

    val updated = authUserRepository.findById(user.id!!).orElseThrow()
    assertThat(matchesDjangoPassword("changed-with-code", updated.password)).isTrue()
    assertThat(passwordResetTokenRepository.findById(code)).isPresent
  }

  private fun userForToken(token: String): AuthUser = oauthAccessTokenRepository.findByToken(token)?.user ?: error("Missing seeded token $token")

  private fun djangoPassword(password: String, salt: String = "testsalt", iterations: Int = 600000): String {
    val hash = pbkdf2(password, salt, iterations)
    return listOf("pbkdf2_sha256", iterations.toString(), salt, hash).joinToString("$")
  }

  private fun matchesDjangoPassword(raw: String, encoded: String): Boolean {
    val parts = encoded.split('$')
    if (parts.size != 4 || parts[0] != "pbkdf2_sha256") return false
    val iterations = parts[1].toInt()
    val salt = parts[2]
    val expected = Base64.getDecoder().decode(parts[3])
    val actual = Base64.getDecoder().decode(pbkdf2(raw, salt, iterations))
    return MessageDigest.isEqual(actual, expected)
  }

  private fun pbkdf2(password: String, salt: String, iterations: Int): String {
    val keySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), iterations, 256)
    val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec)
    return Base64.getEncoder().encodeToString(secret.encoded)
  }
}
