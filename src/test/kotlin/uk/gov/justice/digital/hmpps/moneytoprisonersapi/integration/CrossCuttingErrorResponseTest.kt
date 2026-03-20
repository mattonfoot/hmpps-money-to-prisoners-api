package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository

@DisplayName("14.5 Error Response Conventions")
class CrossCuttingErrorResponseTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var logRepository: LogRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @BeforeEach
  fun setUp() {
    logRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun saveCredit(resolution: CreditResolution = CreditResolution.PENDING, prison: String? = null): Credit {
    val credit = Credit(amount = 1000L, resolution = resolution, prison = prison)
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  @Nested
  @DisplayName("XCT-040 400 Bad Request for validation errors")
  inner class BadRequestErrors {

    @Test
    @DisplayName("XCT-040 empty credit_ids in refund request returns 400")
    fun `empty credit_ids list returns 400 Bad Request`() {
      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("credit_ids" to emptyList<Long>()))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("XCT-040 empty credit_ids in review request returns 400")
    fun `empty credit_ids list in review returns 400 Bad Request`() {
      webTestClient.post()
        .uri("/credits/actions/review/")
        .headers(setAuthorisation())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("credit_ids" to emptyList<Long>()))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("XCT-040 malformed JSON body returns 400")
    fun `malformed JSON body returns 400 Bad Request`() {
      webTestClient.post()
        .uri("/credits/actions/review/")
        .headers(setAuthorisation())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue("{ invalid json }")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("XCT-040 invalid enum value for filter parameter returns 400")
    fun `invalid enum value for resolution filter returns 400`() {
      webTestClient.get()
        .uri("/credits/?resolution=INVALID_RESOLUTION")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  @DisplayName("XCT-041 401 Unauthorized for missing or invalid auth token")
  inner class UnauthorizedErrors {

    @Test
    @DisplayName("XCT-041 GET /credits/ without auth token returns 401")
    fun `request without auth token returns 401 Unauthorized`() {
      webTestClient.get()
        .uri("/credits/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("XCT-041 POST /credits/actions/review/ without auth token returns 401")
    fun `POST request without auth token returns 401 Unauthorized`() {
      webTestClient.post()
        .uri("/credits/actions/review/")
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("credit_ids" to listOf(1L)))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("XCT-041 GET /disbursements/ without auth token returns 401")
    fun `disbursements request without auth returns 401`() {
      webTestClient.get()
        .uri("/disbursements/")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  @DisplayName("XCT-042 403 Forbidden for insufficient permissions")
  inner class ForbiddenErrors {

    @Test
    @DisplayName("XCT-042 accessing auto-accept rules without SECURITY_STAFF role returns 403")
    fun `accessing security auto-accept without required role returns 403 Forbidden`() {
      webTestClient.get()
        .uri("/security/checks/auto-accept/")
        .headers(setAuthorisation(roles = emptyList())) // authenticated but no role
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("XCT-042 accessing monitored email addresses without FIU role returns 403 on POST")
    fun `creating monitored email keyword without FIU role returns 403 Forbidden`() {
      webTestClient.post()
        .uri("/security/monitored-email-addresses/")
        .headers(setAuthorisation(roles = emptyList()))
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("keyword" to "test-keyword"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("XCT-042 accessing prisoner account balance without SEND_MONEY role returns 403")
    fun `accessing prisoner account balance without SEND_MONEY role returns 403 Forbidden`() {
      webTestClient.get()
        .uri("/prisoner_account_balances/A1234BC/")
        .headers(setAuthorisation(roles = emptyList()))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  @DisplayName("XCT-043 404 Not Found for missing resources")
  inner class NotFoundErrors {

    @Test
    @DisplayName("XCT-043 accessing a non-existent path returns 404")
    fun `accessing non-existent endpoint returns 404 Not Found`() {
      webTestClient.get()
        .uri("/this-path-does-not-exist/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    @DisplayName("XCT-043 accessing a valid path with non-existent ID returns 404")
    fun `accessing user with non-existent ID returns 404`() {
      webTestClient.get()
        .uri("/users/999999/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("XCT-044 409 Conflict for invalid state transitions")
  inner class ConflictErrors {

    @Test
    @DisplayName("XCT-044 refund of credit in CREDITED state returns 409 Conflict")
    fun `refund of credit in wrong state returns 409 Conflict`() {
      val credit = saveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("credit_ids" to listOf(credit.id)))
        .exchange()
        .expectStatus().isEqualTo(409)
    }

    @Test
    @DisplayName("XCT-044 409 response body contains status and message")
    fun `409 response includes error details`() {
      val credit = saveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("credit_ids" to listOf(credit.id)))
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.status").isEqualTo(409)
    }

    @Test
    @DisplayName("XCT-044 refund of REFUNDED credit (already refunded) also returns 409")
    fun `refund of already refunded credit returns 409 Conflict`() {
      val credit = saveCredit(resolution = CreditResolution.REFUNDED)

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("credit_ids" to listOf(credit.id)))
        .exchange()
        .expectStatus().isEqualTo(409)
    }
  }
}
