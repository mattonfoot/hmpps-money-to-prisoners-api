package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementCommentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementLogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository

class DisbursementResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var disbursementRepository: DisbursementRepository

  @Autowired
  private lateinit var disbursementLogRepository: DisbursementLogRepository

  @Autowired
  private lateinit var disbursementCommentRepository: DisbursementCommentRepository

  @Autowired
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  @BeforeEach
  fun setUp() {
    disbursementCommentRepository.deleteAll()
    disbursementLogRepository.deleteAll()
    disbursementRepository.deleteAll()
    prisonerProfileRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /disbursements/")
  inner class ListDisbursements {

    @Test
    @DisplayName("DSB-030 - Unauthenticated returns 401")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.get()
        .uri("/disbursements/")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    @DisplayName("DSB-031 - Authenticated user can list disbursements")
    fun `should return 200 for authenticated user`() {
      webTestClient.get()
        .uri("/disbursements/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus()
        .isOk
    }

    @Test
    @DisplayName("DSB-032 - Returns paginated response format")
    fun `should return paginated response format`() {
      disbursementRepository.save(createDisbursement())

      webTestClient.get()
        .uri("/disbursements/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(1)
    }

    @Test
    @DisplayName("DSB-033 - Response includes all fields")
    fun `should include all disbursement fields in response`() {
      disbursementRepository.save(createDisbursement())

      webTestClient.get()
        .uri("/disbursements/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.results[0].id").isNotEmpty
        .jsonPath("$.results[0].amount").isEqualTo(5000)
        .jsonPath("$.results[0].method").isEqualTo("BANK_TRANSFER")
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
        .jsonPath("$.results[0].prisoner_number").isEqualTo("A1234BC")
        .jsonPath("$.results[0].prisoner_name").isEqualTo("John Smith")
        .jsonPath("$.results[0].recipient_first_name").isEqualTo("Jane")
        .jsonPath("$.results[0].recipient_last_name").isEqualTo("Doe")
        .jsonPath("$.results[0].recipient_name").isEqualTo("Jane Doe")
        .jsonPath("$.results[0].resolution").isEqualTo("PENDING")
        .jsonPath("$.results[0].recipient_is_company").isEqualTo(false)
    }

    @Test
    @DisplayName("DSB-060 - Filter by resolution")
    fun `should filter by resolution`() {
      disbursementRepository.save(createDisbursement(resolution = DisbursementResolution.PENDING))
      disbursementRepository.save(createDisbursement(resolution = DisbursementResolution.CONFIRMED))

      webTestClient.get()
        .uri("/disbursements/?resolution=PENDING")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].resolution").isEqualTo("PENDING")
    }

    @Test
    @DisplayName("DSB-061 - Filter by prison")
    fun `should filter by prison`() {
      disbursementRepository.save(createDisbursement(prison = "LEI"))
      disbursementRepository.save(createDisbursement(prison = "MDI"))

      webTestClient.get()
        .uri("/disbursements/?prison=LEI")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
    }

    @Test
    @DisplayName("DSB-062 - Filter by prisoner_number (case-insensitive)")
    fun `should filter by prisoner number case insensitive`() {
      disbursementRepository.save(createDisbursement(prisonerNumber = "A1234BC"))
      disbursementRepository.save(createDisbursement(prisonerNumber = "B5678DE"))

      webTestClient.get()
        .uri("/disbursements/?prisoner_number=a1234bc")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_number").isEqualTo("A1234BC")
    }

    @Test
    @DisplayName("DSB-063 - Filter by amount exact")
    fun `should filter by exact amount`() {
      disbursementRepository.save(createDisbursement(amount = 5000L))
      disbursementRepository.save(createDisbursement(amount = 3000L))

      webTestClient.get()
        .uri("/disbursements/?amount=5000")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(5000)
    }
  }

  @Nested
  @DisplayName("POST /disbursements/")
  inner class CreateDisbursement {

    @Test
    @DisplayName("DSB-034 - POST /disbursements/ creates disbursement and returns 201")
    fun `should create disbursement and return 201`() {
      webTestClient.post()
        .uri("/disbursements/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """{
            "amount": 5000,
            "method": "BANK_TRANSFER",
            "prison": "LEI",
            "prisoner_number": "A1234BC",
            "prisoner_name": "John Smith",
            "recipient_first_name": "Jane",
            "recipient_last_name": "Doe"
          }""",
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("$.id").isNotEmpty
        .jsonPath("$.amount").isEqualTo(5000)
        .jsonPath("$.resolution").isEqualTo("PENDING")
        .jsonPath("$.recipient_name").isEqualTo("Jane Doe")
    }

    @Test
    @DisplayName("DSB-035 - Unauthenticated returns 401")
    fun `should return 401 for unauthenticated`() {
      webTestClient.post()
        .uri("/disbursements/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"amount": 5000, "method": "BANK_TRANSFER", "prison": "LEI", "prisoner_number": "A1234BC", "prisoner_name": "John Smith", "recipient_first_name": "Jane", "recipient_last_name": "Doe"}""")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    @DisplayName("DSB-036 - Missing ROLE_PRISON_CLERK returns 403")
    fun `should return 403 without prison clerk role`() {
      webTestClient.post()
        .uri("/disbursements/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"amount": 5000, "method": "BANK_TRANSFER", "prison": "LEI", "prisoner_number": "A1234BC", "prisoner_name": "John Smith", "recipient_first_name": "Jane", "recipient_last_name": "Doe"}""")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  @DisplayName("PATCH /disbursements/{id}/")
  inner class UpdateDisbursement {

    @Test
    @DisplayName("DSB-037 - PATCH updates PENDING disbursement")
    fun `should update pending disbursement`() {
      val disbursement = disbursementRepository.save(createDisbursement())

      webTestClient.patch()
        .uri("/disbursements/${disbursement.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"amount": 7500}""")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.amount").isEqualTo(7500)
    }

    @Test
    @DisplayName("DSB-038 - PATCH on non-PENDING disbursement returns 400")
    fun `should return 400 when updating non-pending disbursement`() {
      val disbursement = disbursementRepository.save(createDisbursement(resolution = DisbursementResolution.PRECONFIRMED))

      webTestClient.patch()
        .uri("/disbursements/${disbursement.id}/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"amount": 7500}""")
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    @DisplayName("DSB-039 - PATCH on non-existent ID returns 404")
    fun `should return 404 for unknown disbursement`() {
      webTestClient.patch()
        .uri("/disbursements/99999/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"amount": 7500}""")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @DisplayName("POST /disbursements/actions/reject/")
  inner class RejectAction {

    @Test
    @DisplayName("DSB-040 - Reject transitions to REJECTED and returns 204")
    fun `should reject disbursements and return 204`() {
      val disbursement = disbursementRepository.save(createDisbursement())

      webTestClient.post()
        .uri("/disbursements/actions/reject/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"disbursement_ids": [${disbursement.id}]}""")
        .exchange()
        .expectStatus()
        .isNoContent

      val updated = disbursementRepository.findById(disbursement.id!!).get()
      assert(updated.resolution == DisbursementResolution.REJECTED)
    }

    @Test
    @DisplayName("DSB-041 - Invalid transition returns 409")
    fun `should return 409 for invalid state transition`() {
      val disbursement = disbursementRepository.save(createDisbursement(resolution = DisbursementResolution.SENT))

      webTestClient.post()
        .uri("/disbursements/actions/reject/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"disbursement_ids": [${disbursement.id}]}""")
        .exchange()
        .expectStatus()
        .isEqualTo(409)
    }
  }

  @Nested
  @DisplayName("POST /disbursements/actions/preconfirm/")
  inner class PreconfirmAction {

    @Test
    @DisplayName("DSB-042 - Preconfirm transitions to PRECONFIRMED and returns 204")
    fun `should preconfirm disbursements and return 204`() {
      val disbursement = disbursementRepository.save(createDisbursement())

      webTestClient.post()
        .uri("/disbursements/actions/preconfirm/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"disbursement_ids": [${disbursement.id}]}""")
        .exchange()
        .expectStatus()
        .isNoContent

      val updated = disbursementRepository.findById(disbursement.id!!).get()
      assert(updated.resolution == DisbursementResolution.PRECONFIRMED)
    }
  }

  @Nested
  @DisplayName("POST /disbursements/actions/reset/")
  inner class ResetAction {

    @Test
    @DisplayName("DSB-043 - Reset transitions PRECONFIRMED back to PENDING and returns 204")
    fun `should reset disbursements to PENDING and return 204`() {
      val disbursement = disbursementRepository.save(createDisbursement(resolution = DisbursementResolution.PRECONFIRMED))

      webTestClient.post()
        .uri("/disbursements/actions/reset/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"disbursement_ids": [${disbursement.id}]}""")
        .exchange()
        .expectStatus()
        .isNoContent

      val updated = disbursementRepository.findById(disbursement.id!!).get()
      assert(updated.resolution == DisbursementResolution.PENDING)
    }
  }

  @Nested
  @DisplayName("POST /disbursements/actions/confirm/")
  inner class ConfirmAction {

    @Test
    @DisplayName("DSB-044 - Confirm transitions to CONFIRMED and sets invoice number")
    fun `should confirm disbursements and return 204`() {
      val disbursement = disbursementRepository.save(createDisbursement(resolution = DisbursementResolution.PRECONFIRMED))

      webTestClient.post()
        .uri("/disbursements/actions/confirm/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"disbursements": [{"id": ${disbursement.id}, "nomis_transaction_id": "TXN001"}]}""")
        .exchange()
        .expectStatus()
        .isNoContent

      val updated = disbursementRepository.findById(disbursement.id!!).get()
      assert(updated.resolution == DisbursementResolution.CONFIRMED)
      assert(updated.invoiceNumber == "PMD${1000000 + disbursement.id!!}")
    }
  }

  @Nested
  @DisplayName("POST /disbursements/actions/send/")
  inner class SendAction {

    @Test
    @DisplayName("DSB-045 - Send transitions to SENT and returns 204")
    fun `should send disbursements and return 204`() {
      val disbursement = disbursementRepository.save(createDisbursement(resolution = DisbursementResolution.CONFIRMED))

      webTestClient.post()
        .uri("/disbursements/actions/send/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"disbursement_ids": [${disbursement.id}]}""")
        .exchange()
        .expectStatus()
        .isNoContent

      val updated = disbursementRepository.findById(disbursement.id!!).get()
      assert(updated.resolution == DisbursementResolution.SENT)
    }

    @Test
    @DisplayName("DSB-046 - Send without ROLE_BANK_ADMIN returns 403")
    fun `should return 403 without bank admin role`() {
      val disbursement = disbursementRepository.save(createDisbursement(resolution = DisbursementResolution.CONFIRMED))

      webTestClient.post()
        .uri("/disbursements/actions/send/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"disbursement_ids": [${disbursement.id}]}""")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  @DisplayName("POST /disbursements/comments/")
  inner class CreateComments {

    @Test
    @DisplayName("DSB-080 - Create comments returns 201")
    fun `should create comments on disbursements`() {
      val disbursement = disbursementRepository.save(createDisbursement())

      webTestClient.post()
        .uri("/disbursements/comments/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"disbursement": ${disbursement.id}, "comment": "Test comment"}]""")
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("$[0].id").isNotEmpty
        .jsonPath("$[0].comment").isEqualTo("Test comment")
    }

    @Test
    @DisplayName("DSB-081 - Comment exceeding 3000 chars returns 400")
    fun `should return 400 for comment exceeding 3000 characters`() {
      val disbursement = disbursementRepository.save(createDisbursement())
      val longComment = "x".repeat(3001)

      webTestClient.post()
        .uri("/disbursements/comments/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"disbursement": ${disbursement.id}, "comment": "$longComment"}]""")
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    @DisplayName("DSB-082 - Missing ROLE_PRISON_CLERK returns 403")
    fun `should return 403 without prison clerk role`() {
      val disbursement = disbursementRepository.save(createDisbursement())

      webTestClient.post()
        .uri("/disbursements/comments/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"disbursement": ${disbursement.id}, "comment": "Test"}]""")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    @DisplayName("DSB-083 - Comment with optional category")
    fun `should create comment with optional category`() {
      val disbursement = disbursementRepository.save(createDisbursement())

      webTestClient.post()
        .uri("/disbursements/comments/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"disbursement": ${disbursement.id}, "comment": "Test comment", "category": "GENERAL"}]""")
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("$[0].category").isEqualTo("GENERAL")
    }
  }

  @Nested
  @DisplayName("GET /disbursements/?monitored=True (MonitoredDisbursementListTestCase)")
  inner class MonitoredDisbursements {

    @Test
    @DisplayName("DSB-090 - Returns 401 for unauthenticated monitored filter request")
    fun `should return 401 for unauthenticated monitored request`() {
      webTestClient.get()
        .uri("/disbursements/?monitored=true")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("DSB-091 - Returns only disbursements for monitored prisoners when monitored=true")
    fun `should return disbursements for monitored prisoners`() {
      // Create a prisoner profile monitored by security_user
      transactionTemplate.execute {
        val profile = PrisonerProfile(prisonerNumber = "A1234BC", prisonerName = "John Smith")
        profile.monitoringUsers.add("security_user")
        prisonerProfileRepository.save(profile)
      }

      // Disbursement for monitored prisoner
      disbursementRepository.save(createDisbursement(prisonerNumber = "A1234BC", amount = 5000L))
      // Disbursement for unmonitored prisoner
      disbursementRepository.save(createDisbursement(prisonerNumber = "Z9999ZZ", amount = 2000L))

      webTestClient.get()
        .uri("/disbursements/?monitored=true")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(5000)
    }

    @Test
    @DisplayName("DSB-092 - Returns empty list when no prisoners monitored by user")
    fun `should return empty list when no monitored prisoners`() {
      disbursementRepository.save(createDisbursement(prisonerNumber = "A1234BC"))

      webTestClient.get()
        .uri("/disbursements/?monitored=true")
        .headers(setAuthorisation(username = "security_user", roles = listOf("ROLE_SECURITY_STAFF")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }
  }

  private fun createDisbursement(
    amount: Long = 5000L,
    prison: String = "LEI",
    prisonerNumber: String = "A1234BC",
    resolution: DisbursementResolution = DisbursementResolution.PENDING,
  ) = Disbursement(
    amount = amount,
    method = DisbursementMethod.BANK_TRANSFER,
    prison = prison,
    prisonerNumber = prisonerNumber,
    prisonerName = "John Smith",
    recipientFirstName = "Jane",
    recipientLastName = "Doe",
    resolution = resolution,
  )
}
