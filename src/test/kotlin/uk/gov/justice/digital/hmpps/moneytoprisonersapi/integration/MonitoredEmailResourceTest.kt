package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MonitoredPartialEmailAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MonitoredPartialEmailAddressRepository

class MonitoredEmailResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: MonitoredPartialEmailAddressRepository

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Nested
  @DisplayName("GET /security/monitored-email-addresses/ (SEC-113)")
  inner class ListKeywords {

    @Test
    @DisplayName("SEC-113 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/security/monitored-email-addresses/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-113 - Returns list of keywords alphabetically")
    fun `should return keywords in alphabetical order`() {
      repository.save(MonitoredPartialEmailAddress(keyword = "zebra"))
      repository.save(MonitoredPartialEmailAddress(keyword = "apple"))

      webTestClient.get()
        .uri("/security/monitored-email-addresses/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0]").isEqualTo("apple")
        .jsonPath("$[1]").isEqualTo("zebra")
    }
  }

  @Nested
  @DisplayName("POST /security/monitored-email-addresses/ (SEC-110 to SEC-112)")
  inner class CreateKeyword {

    @Test
    @DisplayName("SEC-110 - Creates a keyword with ROLE_FIU")
    fun `should create a keyword`() {
      webTestClient.post()
        .uri("/security/monitored-email-addresses/")
        .headers(setAuthorisation(roles = listOf("ROLE_FIU")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"keyword": "Fraud"}""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.keyword").isEqualTo("fraud")

      assertThat(repository.count()).isEqualTo(1L)
      assertThat(repository.findAll().first().keyword).isEqualTo("fraud")
    }

    @Test
    @DisplayName("SEC-111 - Returns 403 without ROLE_FIU")
    fun `should return 403 without FIU role`() {
      webTestClient.post()
        .uri("/security/monitored-email-addresses/")
        .headers(setAuthorisation(roles = listOf("ROLE_SECURITY_STAFF")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"keyword": "fraud"}""")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("SEC-111 - Returns 400 for keyword shorter than 3 characters")
    fun `should return 400 for short keyword`() {
      webTestClient.post()
        .uri("/security/monitored-email-addresses/")
        .headers(setAuthorisation(roles = listOf("ROLE_FIU")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"keyword": "ab"}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("SEC-112 - Returns 400 for duplicate keyword")
    fun `should return 400 for duplicate keyword`() {
      repository.save(MonitoredPartialEmailAddress(keyword = "fraud"))

      webTestClient.post()
        .uri("/security/monitored-email-addresses/")
        .headers(setAuthorisation(roles = listOf("ROLE_FIU")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"keyword": "fraud"}""")
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  @DisplayName("DELETE /security/monitored-email-addresses/{keyword}/ (SEC-117)")
  inner class DeleteKeyword {

    @Test
    @DisplayName("SEC-117 - Deletes a keyword with ROLE_FIU")
    fun `should delete a keyword`() {
      repository.save(MonitoredPartialEmailAddress(keyword = "fraud"))

      webTestClient.delete()
        .uri("/security/monitored-email-addresses/fraud/")
        .headers(setAuthorisation(roles = listOf("ROLE_FIU")))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.count()).isEqualTo(0L)
    }

    @Test
    @DisplayName("SEC-117 - Returns 404 when keyword not found")
    fun `should return 404 when keyword not found`() {
      webTestClient.delete()
        .uri("/security/monitored-email-addresses/notexist/")
        .headers(setAuthorisation(roles = listOf("ROLE_FIU")))
        .exchange()
        .expectStatus().isNotFound
    }
  }
}
