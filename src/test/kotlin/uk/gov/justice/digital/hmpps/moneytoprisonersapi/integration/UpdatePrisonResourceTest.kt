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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

class UpdatePrisonResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @Autowired
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Autowired
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @BeforeEach
  fun setUp() {
    privateEstateBatchRepository.deleteAll()
    senderProfileRepository.deleteAll()
    prisonerProfileRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun createAndSavePrison(nomisId: String = "LEI"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Leeds", region = "Yorkshire")
    return prisonRepository.save(prison)
  }

  private fun createAndSaveCredit(
    prisonerNumber: String? = "A1234BC",
    prison: String? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
  ): Credit {
    val credit = Credit(
      amount = 1000,
      prisonerNumber = prisonerNumber,
      prisonerName = "John Smith",
      prison = prison,
      resolution = resolution,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  @Nested
  @DisplayName("POST /credits/actions/update-prison/ (CRD-220 to CRD-223)")
  inner class UpdatePrisonAction {

    @Test
    @DisplayName("CRD-223 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.post()
        .uri("/credits/actions/update-prison/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"prisoner_number": "A1234BC", "prison": "LEI"}""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-220 - Sets prison on credits matching prisoner number")
    fun `should set prison on credits matching prisoner number`() {
      createAndSavePrison("LEI")
      val credit = createAndSaveCredit(prisonerNumber = "A1234BC", prison = null)

      webTestClient.post()
        .uri("/credits/actions/update-prison/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(credit.id!!).get().prison).isEqualTo("LEI")
    }

    @Test
    @DisplayName("CRD-220 - Sets prison on multiple credits with the same prisoner number")
    fun `should set prison on multiple credits with the same prisoner number`() {
      createAndSavePrison("LEI")
      val credit1 = createAndSaveCredit(prisonerNumber = "A1234BC", prison = null)
      val credit2 = createAndSaveCredit(prisonerNumber = "A1234BC", prison = null)

      webTestClient.post()
        .uri("/credits/actions/update-prison/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(credit1.id!!).get().prison).isEqualTo("LEI")
      assertThat(creditRepository.findById(credit2.id!!).get().prison).isEqualTo("LEI")
    }

    @Test
    @DisplayName("CRD-221 - Only updates credits with no prison assigned")
    fun `should only update credits with no prison assigned`() {
      createAndSavePrison("LEI")
      createAndSavePrison("MDI")
      val creditNoPrison = createAndSaveCredit(prisonerNumber = "A1234BC", prison = null)
      val creditWithPrison = createAndSaveCredit(prisonerNumber = "A1234BC", prison = "MDI")

      webTestClient.post()
        .uri("/credits/actions/update-prison/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(creditNoPrison.id!!).get().prison).isEqualTo("LEI")
      // Credit already with a prison is not changed
      assertThat(creditRepository.findById(creditWithPrison.id!!).get().prison).isEqualTo("MDI")
    }

    @Test
    @DisplayName("CRD-220 - Handles multiple prisoner_number/prison pairs in one request")
    fun `should handle multiple pairs in one request`() {
      createAndSavePrison("LEI")
      createAndSavePrison("MDI")
      val credit1 = createAndSaveCredit(prisonerNumber = "A1234BC", prison = null)
      val credit2 = createAndSaveCredit(prisonerNumber = "B5678DE", prison = null)

      webTestClient.post()
        .uri("/credits/actions/update-prison/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue(
          """[
            {"prisoner_number": "A1234BC", "prison": "LEI"},
            {"prisoner_number": "B5678DE", "prison": "MDI"}
          ]""",
        )
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(credit1.id!!).get().prison).isEqualTo("LEI")
      assertThat(creditRepository.findById(credit2.id!!).get().prison).isEqualTo("MDI")
    }

    @Test
    @DisplayName("CRD-222 - Empty list returns 204 no content")
    fun `should return 204 for empty list`() {
      webTestClient.post()
        .uri("/credits/actions/update-prison/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("[]")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("CRD-220 - Does not update credits for non-matching prisoner numbers")
    fun `should not update credits for non-matching prisoner numbers`() {
      createAndSavePrison("LEI")
      val credit = createAndSaveCredit(prisonerNumber = "Z9999ZZ", prison = null)

      webTestClient.post()
        .uri("/credits/actions/update-prison/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(credit.id!!).get().prison).isNull()
    }
  }
}
