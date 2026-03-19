package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CommentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository

class CommentResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var commentRepository: CommentRepository

  @BeforeEach
  fun setUp() {
    commentRepository.deleteAll()
    creditRepository.deleteAll()
  }

  private fun createAndSaveCredit(): Credit {
    val credit = Credit(
      amount = 1000,
      prisonerNumber = "A1234BC",
      prisonerName = "John Smith",
      resolution = CreditResolution.CREDITED,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  @Nested
  @DisplayName("POST /comments/ (CRD-160 to CRD-164)")
  inner class CreateComments {

    @Test
    @DisplayName("CRD-160 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.post()
        .uri("/comments/")
        .header("Content-Type", "application/json")
        .bodyValue("[]")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-160 - POST /comments/ creates comments from array")
    fun `should create comments from array`() {
      val credit = createAndSaveCredit()

      webTestClient.post()
        .uri("/comments/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"credit": ${credit.id}, "comment": "Test comment"}]""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").isArray
        .jsonPath("$[0].credit").isEqualTo(credit.id!!.toInt())
        .jsonPath("$[0].comment").isEqualTo("Test comment")
    }

    @Test
    @DisplayName("CRD-160 - POST /comments/ persists multiple comments")
    fun `should persist multiple comments`() {
      val credit1 = createAndSaveCredit()
      val credit2 = createAndSaveCredit()

      webTestClient.post()
        .uri("/comments/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue(
          """[
            {"credit": ${credit1.id}, "comment": "First comment"},
            {"credit": ${credit2.id}, "comment": "Second comment"}
          ]""",
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.length()").isEqualTo(2)

      assertThat(commentRepository.count()).isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-161 - Comment max 3000 chars - validation")
    fun `should reject comment exceeding 3000 chars`() {
      val credit = createAndSaveCredit()
      val longComment = "A".repeat(3001)

      webTestClient.post()
        .uri("/comments/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"credit": ${credit.id}, "comment": "$longComment"}]""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("CRD-161 - Comment at exactly 3000 chars is accepted")
    fun `should accept comment at exactly 3000 chars`() {
      val credit = createAndSaveCredit()
      val maxComment = "A".repeat(3000)

      webTestClient.post()
        .uri("/comments/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"credit": ${credit.id}, "comment": "$maxComment"}]""")
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    @DisplayName("CRD-162 - User is auto-set to request user (read-only)")
    fun `should auto-set user to authenticated user`() {
      val credit = createAndSaveCredit()

      webTestClient.post()
        .uri("/comments/")
        .headers(setAuthorisation(username = "auth_user"))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"credit": ${credit.id}, "comment": "My comment"}]""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$[0].user_id").isEqualTo("auth_user")
    }

    @Test
    @DisplayName("CRD-164 - Returns 201 Created with comment data")
    fun `should return 201 with created comment data`() {
      val credit = createAndSaveCredit()

      webTestClient.post()
        .uri("/comments/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"credit": ${credit.id}, "comment": "Important note"}]""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$[0].id").isNotEmpty
        .jsonPath("$[0].credit").isEqualTo(credit.id!!.toInt())
        .jsonPath("$[0].comment").isEqualTo("Important note")
        .jsonPath("$[0].user_id").isEqualTo("clerk1")
        .jsonPath("$[0].created").isNotEmpty
    }

    @Test
    @DisplayName("CRD-160 - Empty array is accepted and returns empty list")
    fun `should accept empty array and return empty list`() {
      webTestClient.post()
        .uri("/comments/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("[]")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").isArray
        .jsonPath("$.length()").isEqualTo(0)
    }
  }
}
