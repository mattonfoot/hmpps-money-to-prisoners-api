package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerCreditNoticeEmail
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerCreditNoticeEmailRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerLocationRepository

class PrisonerCreditNoticeEmailResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var prisonerCreditNoticeEmailRepository: PrisonerCreditNoticeEmailRepository

  @Autowired
  private lateinit var prisonerLocationRepository: PrisonerLocationRepository

  @BeforeEach
  fun setUp() {
    prisonerLocationRepository.deleteAll()
    prisonerCreditNoticeEmailRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun createPrison(nomisId: String = "LEI"): Prison = prisonRepository.save(Prison(nomisId = nomisId, name = "Test Prison", region = "Test Region"))

  private fun createEmail(prison: Prison, email: String): PrisonerCreditNoticeEmail = prisonerCreditNoticeEmailRepository.save(PrisonerCreditNoticeEmail(prison = prison, email = email))

  @Nested
  @DisplayName("GET /prisoner_credit_notice_email/")
  inner class ListEmails {

    @Test
    @DisplayName("PRS-070 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.get()
        .uri("/prisoner_credit_notice_email/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-071 - Without ROLE_PRISON_CLERK or ROLE_CASHBOOK returns 403")
    fun `should return 403 without required role`() {
      webTestClient.get()
        .uri("/prisoner_credit_notice_email/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PRS-071 - ROLE_PRISON_CLERK can list emails")
    fun `should return 200 with ROLE_PRISON_CLERK`() {
      webTestClient.get()
        .uri("/prisoner_credit_notice_email/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    @DisplayName("PRS-071 - ROLE_CASHBOOK can list emails")
    fun `should return 200 with ROLE_CASHBOOK`() {
      webTestClient.get()
        .uri("/prisoner_credit_notice_email/")
        .headers(setAuthorisation(roles = listOf("ROLE_CASHBOOK")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    @DisplayName("PRS-072 - Returns list of all notice emails")
    fun `should return list of all notice emails`() {
      val prison1 = createPrison("LEI")
      val prison2 = createPrison("MDI")
      createEmail(prison1, "leeds@test.com")
      createEmail(prison2, "moorland@test.com")

      webTestClient.get()
        .uri("/prisoner_credit_notice_email/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results").isArray
    }

    @Test
    @DisplayName("PRS-072 - Returns expected fields")
    fun `should return expected fields in response`() {
      val prison = createPrison("LEI")
      createEmail(prison, "test@example.com")

      webTestClient.get()
        .uri("/prisoner_credit_notice_email/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
        .jsonPath("$.results[0].email").isEqualTo("test@example.com")
    }
  }

  @Nested
  @DisplayName("POST /prisoner_credit_notice_email/")
  inner class CreateEmail {

    @Test
    @DisplayName("PRS-073 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.post()
        .uri("/prisoner_credit_notice_email/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"prison": "LEI", "email": "test@example.com"}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-073 - Without ROLE_PRISON_CLERK returns 403")
    fun `should return 403 without ROLE_PRISON_CLERK`() {
      webTestClient.post()
        .uri("/prisoner_credit_notice_email/")
        .headers(setAuthorisation(roles = listOf("ROLE_CASHBOOK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"prison": "LEI", "email": "test@example.com"}""")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PRS-073 - ROLE_PRISON_CLERK can create email config")
    fun `should create email config with ROLE_PRISON_CLERK`() {
      createPrison("LEI")

      webTestClient.post()
        .uri("/prisoner_credit_notice_email/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"prison": "LEI", "email": "test@example.com"}""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.prison").isEqualTo("LEI")
        .jsonPath("$.email").isEqualTo("test@example.com")
    }
  }

  @Nested
  @DisplayName("PATCH /prisoner_credit_notice_email/{prison_id}/")
  inner class UpdateEmail {

    @Test
    @DisplayName("PRS-074 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.patch()
        .uri("/prisoner_credit_notice_email/LEI/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"email": "new@example.com"}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-074 - Updates email for prison")
    fun `should update email for prison`() {
      val prison = createPrison("LEI")
      createEmail(prison, "old@example.com")

      webTestClient.patch()
        .uri("/prisoner_credit_notice_email/LEI/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"email": "new@example.com"}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.email").isEqualTo("new@example.com")
    }

    @Test
    @DisplayName("PRS-075 - Invalid email returns 400")
    fun `should return 400 for invalid email`() {
      val prison = createPrison("LEI")
      createEmail(prison, "old@example.com")

      webTestClient.patch()
        .uri("/prisoner_credit_notice_email/LEI/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"email": "not-an-email"}""")
        .exchange()
        .expectStatus().isBadRequest
    }
  }
}
