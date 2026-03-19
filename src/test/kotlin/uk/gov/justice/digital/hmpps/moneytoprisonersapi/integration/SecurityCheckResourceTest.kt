package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SecurityCheckRepository

class SecurityCheckResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var securityCheckRepository: SecurityCheckRepository

  @BeforeEach
  fun setUp() {
    securityCheckRepository.deleteAll()
    creditRepository.deleteAll()
  }

  private fun createCreditWithCheck(status: CheckStatus = CheckStatus.PENDING): SecurityCheck {
    val credit = creditRepository.save(
      Credit(amount = 1000, resolution = CreditResolution.PENDING),
    )
    val check = SecurityCheck(credit = credit, status = status)
    return securityCheckRepository.save(check)
  }

  @Nested
  @DisplayName("GET /security/checks/ (SEC-050 to SEC-059)")
  inner class ListChecks {

    @Test
    @DisplayName("SEC-050 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/security/checks/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-051 - Returns 403 for user without required role")
    fun `should return 403 for user without security role`() {
      webTestClient.get()
        .uri("/security/checks/")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("SEC-052 - Returns paginated list for ROLE_SECURITY_STAFF")
    fun `should return checks for security staff role`() {
      createCreditWithCheck(CheckStatus.PENDING)
      createCreditWithCheck(CheckStatus.ACCEPTED)

      webTestClient.get()
        .uri("/security/checks/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results").isArray
    }

    @Test
    @DisplayName("SEC-053 - Filters checks by status=PENDING")
    fun `should filter checks by status`() {
      createCreditWithCheck(CheckStatus.PENDING)
      createCreditWithCheck(CheckStatus.ACCEPTED)

      webTestClient.get()
        .uri("/security/checks/?status=PENDING")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("PENDING")
    }
  }

  @Nested
  @DisplayName("POST /security/checks/{id}/accept/ (SEC-020 to SEC-025)")
  inner class AcceptCheck {

    @Test
    @DisplayName("SEC-020 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated accept`() {
      val check = createCreditWithCheck()

      webTestClient.post()
        .uri("/security/checks/${check.id}/accept/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Known sender"}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-021 - Returns 403 for user without required role")
    fun `should return 403 for accept without security role`() {
      val check = createCreditWithCheck()

      webTestClient.post()
        .uri("/security/checks/${check.id}/accept/")
        .headers(setAuthorisation(roles = listOf()))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Known sender"}""")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("SEC-022 - Accepts a pending check and returns 204")
    fun `should accept a pending check`() {
      val check = createCreditWithCheck(CheckStatus.PENDING)

      webTestClient.post()
        .uri("/security/checks/${check.id}/accept/")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Known sender"}""")
        .exchange()
        .expectStatus().isNoContent

      val updated = securityCheckRepository.findById(check.id!!).get()
      assertThat(updated.status).isEqualTo(CheckStatus.ACCEPTED)
      assertThat(updated.decisionReason).isEqualTo("Known sender")
      assertThat(updated.actionedBy).isEqualTo("security_user")
      assertThat(updated.actionedAt).isNotNull
    }

    @Test
    @DisplayName("SEC-023 - Already accepted is idempotent, returns 204")
    fun `should be idempotent for already accepted check`() {
      val check = createCreditWithCheck(CheckStatus.ACCEPTED)

      webTestClient.post()
        .uri("/security/checks/${check.id}/accept/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Known sender"}""")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("SEC-024 - Already rejected returns 400")
    fun `should return 400 when accepting an already rejected check`() {
      val check = createCreditWithCheck(CheckStatus.REJECTED)

      webTestClient.post()
        .uri("/security/checks/${check.id}/accept/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Known sender"}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("SEC-025 - Missing decision_reason returns 400")
    fun `should return 400 when decision_reason is missing`() {
      val check = createCreditWithCheck()

      webTestClient.post()
        .uri("/security/checks/${check.id}/accept/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": ""}""")
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  @DisplayName("POST /security/checks/{id}/reject/ (SEC-026 to SEC-030)")
  inner class RejectCheck {

    @Test
    @DisplayName("SEC-026 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated reject`() {
      val check = createCreditWithCheck()

      webTestClient.post()
        .uri("/security/checks/${check.id}/reject/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Suspicious", "rejection_reasons": ["FIUMONP"]}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-027 - Rejects a pending check and returns 204")
    fun `should reject a pending check`() {
      val check = createCreditWithCheck(CheckStatus.PENDING)

      webTestClient.post()
        .uri("/security/checks/${check.id}/reject/")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Suspicious activity", "rejection_reasons": ["FIUMONP"]}""")
        .exchange()
        .expectStatus().isNoContent

      val updated = securityCheckRepository.findById(check.id!!).get()
      assertThat(updated.status).isEqualTo(CheckStatus.REJECTED)
      assertThat(updated.decisionReason).isEqualTo("Suspicious activity")
      assertThat(updated.actionedBy).isEqualTo("security_user")
      assertThat(updated.rejectionReasons).contains("FIUMONP")
    }

    @Test
    @DisplayName("SEC-028 - Already rejected is idempotent, returns 204")
    fun `should be idempotent for already rejected check`() {
      val check = createCreditWithCheck(CheckStatus.REJECTED)

      webTestClient.post()
        .uri("/security/checks/${check.id}/reject/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Still suspicious", "rejection_reasons": ["FIUMONP"]}""")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("SEC-029 - Already accepted returns 400")
    fun `should return 400 when rejecting an already accepted check`() {
      val check = createCreditWithCheck(CheckStatus.ACCEPTED)

      webTestClient.post()
        .uri("/security/checks/${check.id}/reject/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Wait", "rejection_reasons": ["FIUMONP"]}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("SEC-030 - Empty rejection_reasons returns 400")
    fun `should return 400 when rejection_reasons is empty`() {
      val check = createCreditWithCheck()

      webTestClient.post()
        .uri("/security/checks/${check.id}/reject/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"decision_reason": "Suspicious", "rejection_reasons": []}""")
        .exchange()
        .expectStatus().isBadRequest
    }
  }
}
