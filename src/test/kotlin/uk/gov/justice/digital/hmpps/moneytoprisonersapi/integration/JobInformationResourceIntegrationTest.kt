package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.JobInformation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.JobInformationRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.OAuthAccessTokenRepository

class JobInformationResourceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var jobInformationRepository: JobInformationRepository

  @Autowired
  private lateinit var oauthAccessTokenRepository: OAuthAccessTokenRepository

  @Test
  @DisplayName("AUTH-070 - POST /job-information/ creates a row for authenticated user")
  fun `should create job information`() {
    webTestClient.post()
      .uri("/job-information/")
      .header("Authorization", "Bearer test-token-security")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("""{"title":"Warden","prison_estate":"Private","tasks":"Run the show"}""")
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.title").isEqualTo("Warden")
      .jsonPath("$.prison_estate").isEqualTo("Private")
      .jsonPath("$.tasks").isEqualTo("Run the show")
  }

  @Test
  @DisplayName("AUTH-072 - duplicate POST /job-information/ returns 500 instead of upserting")
  fun `should fail duplicate job information create`() {
    val user = userForToken("test-token-security")
    jobInformationRepository.save(
      JobInformation().apply {
        this.user = user
        this.title = "Existing title"
        this.prisonEstate = "Existing estate"
        this.tasks = "Existing tasks"
      },
    )

    webTestClient.post()
      .uri("/job-information/")
      .header("Authorization", "Bearer test-token-security")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue("""{"title":"Warden","prison_estate":"Private","tasks":"Run the show"}""")
      .exchange()
      .expectStatus().is5xxServerError
  }

  private fun userForToken(token: String): MtpUser = oauthAccessTokenRepository.findByToken(token)?.user ?: error("Missing seeded token $token")
}
