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
import java.time.LocalDateTime

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

  private fun createCreditWithCheck(
    status: CheckStatus = CheckStatus.PENDING,
    ruleCodes: String? = null,
    startedAt: LocalDateTime? = null,
    actionedBy: String? = null,
    creditResolution: CreditResolution = CreditResolution.PENDING,
  ): SecurityCheck {
    val credit = creditRepository.save(
      Credit(amount = 1000, resolution = creditResolution),
    )
    val check = SecurityCheck(credit = credit, status = status, ruleCodes = ruleCodes, startedAt = startedAt, actionedBy = actionedBy)
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

    @Test
    @DisplayName("SEC-054 - Filters checks by rules code (substring match)")
    fun `should filter checks by rules`() {
      createCreditWithCheck(ruleCodes = "[\"FIUMONP\"]")
      createCreditWithCheck(ruleCodes = "[\"CSFREQ\"]")
      createCreditWithCheck(ruleCodes = null)

      webTestClient.get()
        .uri("/security/checks/?rules=FIUMONP")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("SEC-055 - Filters checks by started_at__gte (inclusive)")
    fun `should filter checks by started_at__gte`() {
      val threshold = LocalDateTime.of(2024, 6, 1, 12, 0, 0)
      createCreditWithCheck(startedAt = threshold.minusHours(1))
      createCreditWithCheck(startedAt = threshold)
      createCreditWithCheck(startedAt = threshold.plusHours(1))

      webTestClient.get()
        .uri("/security/checks/?started_at__gte=2024-06-01T12:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("SEC-056 - Filters checks by started_at__lt (exclusive)")
    fun `should filter checks by started_at__lt`() {
      val threshold = LocalDateTime.of(2024, 6, 1, 12, 0, 0)
      createCreditWithCheck(startedAt = threshold.minusHours(1))
      createCreditWithCheck(startedAt = threshold)
      createCreditWithCheck(startedAt = threshold.plusHours(1))

      webTestClient.get()
        .uri("/security/checks/?started_at__lt=2024-06-01T12:00:00")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("SEC-057 - Filters checks by actioned_by__isnull=false (only actioned checks)")
    fun `should filter checks where actioned_by is not null`() {
      createCreditWithCheck(actionedBy = "security_user")
      createCreditWithCheck(actionedBy = null)

      webTestClient.get()
        .uri("/security/checks/?actioned_by__isnull=false")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("SEC-058 - Filters checks by credit_resolution")
    fun `should filter checks by credit resolution`() {
      createCreditWithCheck(creditResolution = CreditResolution.PENDING)
      createCreditWithCheck(creditResolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/security/checks/?credit_resolution=CREDITED")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
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

  @Nested
  @DisplayName("GET /security/checks/{id}/ (GetCheckTestCase)")
  inner class GetCheck {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated get`() {
      val check = createCreditWithCheck()
      webTestClient.get()
        .uri("/security/checks/${check.id}/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Returns 403 for user without required role")
    fun `should return 403 without security role`() {
      val check = createCreditWithCheck()
      webTestClient.get()
        .uri("/security/checks/${check.id}/")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("Returns 200 with check details")
    fun `should return check details`() {
      val check = createCreditWithCheck(CheckStatus.PENDING, ruleCodes = "[\"FIUMONP\"]")

      webTestClient.get()
        .uri("/security/checks/${check.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.id").isEqualTo(check.id!!.toInt())
        .jsonPath("$.status").isEqualTo("PENDING")
    }

    @Test
    @DisplayName("Returns 404 for non-existent check")
    fun `should return 404 for non-existent check`() {
      webTestClient.get()
        .uri("/security/checks/99999/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("PATCH /security/checks/{id}/ (PatchCheckTestCase)")
  inner class PatchCheck {

    @Test
    @DisplayName("Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated patch`() {
      val check = createCreditWithCheck()
      webTestClient.patch()
        .uri("/security/checks/${check.id}/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"assigned_to": "security_user"}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("Assigns check to a user")
    fun `should assign check to user`() {
      val check = createCreditWithCheck(CheckStatus.PENDING)

      webTestClient.patch()
        .uri("/security/checks/${check.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"assigned_to": "security_user"}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.assigned_to").isEqualTo("security_user")

      val updated = securityCheckRepository.findById(check.id!!).get()
      assertThat(updated.assignedTo).isEqualTo("security_user")
    }

    @Test
    @DisplayName("Returns 400 when reassigning an already-assigned check")
    fun `should return 400 when check already assigned`() {
      val check = createCreditWithCheck(CheckStatus.PENDING)
      val savedCheck = securityCheckRepository.save(check.apply { assignedTo = "existing_user" })

      webTestClient.patch()
        .uri("/security/checks/${savedCheck.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"assigned_to": "other_user"}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("Allows reassignment after clearing assigned_to with null")
    fun `should allow reassignment after clearing to null`() {
      val check = createCreditWithCheck(CheckStatus.PENDING)
      securityCheckRepository.save(check.apply { assignedTo = "existing_user" })

      // Clear the assignment
      webTestClient.patch()
        .uri("/security/checks/${check.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"assigned_to": null}""")
        .exchange()
        .expectStatus().isOk

      // Now reassign
      webTestClient.patch()
        .uri("/security/checks/${check.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"assigned_to": "new_user"}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.assigned_to").isEqualTo("new_user")
    }

    @Test
    @DisplayName("Returns 404 for non-existent check")
    fun `should return 404 for non-existent check on patch`() {
      webTestClient.patch()
        .uri("/security/checks/99999/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"assigned_to": "user"}""")
        .exchange()
        .expectStatus().isNotFound
    }
  }
}
