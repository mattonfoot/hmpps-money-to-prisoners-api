package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerLocation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerLocationRepository
import java.time.LocalDate

class PrisonerValidityResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var prisonerLocationRepository: PrisonerLocationRepository

  @BeforeEach
  fun setUp() {
    prisonerLocationRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun createPrison(nomisId: String = "LEI"): Prison = prisonRepository.save(Prison(nomisId = nomisId, name = "Test Prison", region = "Test Region"))

  private fun createLocation(prisonerNumber: String, prison: Prison, active: Boolean = true, dob: LocalDate? = null): PrisonerLocation {
    val loc = PrisonerLocation(prisonerNumber = prisonerNumber, prison = prison, active = active, createdBy = "test", prisonerDob = dob)
    return prisonerLocationRepository.save(loc)
  }

  @Nested
  @DisplayName("GET /prisoner_validity/")
  inner class PrisonerValidity {

    @Test
    @DisplayName("PRS-050 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.get()
        .uri("/prisoner_validity/?prisoner_number=A1234BC&prisoner_dob=1990-01-01")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-051 - Without ROLE_SEND_MONEY returns 403")
    fun `should return 403 without ROLE_SEND_MONEY`() {
      webTestClient.get()
        .uri("/prisoner_validity/?prisoner_number=A1234BC&prisoner_dob=1990-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_CASHBOOK")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PRS-052 - Missing prisoner_number returns 400")
    fun `should return 400 when prisoner_number is missing`() {
      webTestClient.get()
        .uri("/prisoner_validity/?prisoner_dob=1990-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("PRS-053 - Missing prisoner_dob returns 400")
    fun `should return 400 when prisoner_dob is missing`() {
      webTestClient.get()
        .uri("/prisoner_validity/?prisoner_number=A1234BC")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("PRS-054 - Returns count=1 when matching active location found")
    fun `should return count 1 when prisoner has active location with matching dob`() {
      val prison = createPrison("LEI")
      createLocation("A1234BC", prison, active = true, dob = LocalDate.of(1990, 1, 1))

      webTestClient.get()
        .uri("/prisoner_validity/?prisoner_number=A1234BC&prisoner_dob=1990-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results").isArray
    }

    @Test
    @DisplayName("PRS-055 - Returns count=0 when no matching active location")
    fun `should return count 0 when prisoner not found`() {
      webTestClient.get()
        .uri("/prisoner_validity/?prisoner_number=Z9999XX&prisoner_dob=1990-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(0)
    }

    @Test
    @DisplayName("PRS-055 - Returns count=0 when prisoner has active location but DOB does not match")
    fun `should return count 0 when dob does not match`() {
      val prison = createPrison("LEI")
      createLocation("A1234BC", prison, active = true, dob = LocalDate.of(1990, 1, 1))

      webTestClient.get()
        .uri("/prisoner_validity/?prisoner_number=A1234BC&prisoner_dob=2000-12-31")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("PRS-055 - Returns count=0 when prisoner has inactive location only")
    fun `should return count 0 when only inactive location exists`() {
      val prison = createPrison("LEI")
      createLocation("A1234BC", prison, active = false, dob = LocalDate.of(1990, 1, 1))

      webTestClient.get()
        .uri("/prisoner_validity/?prisoner_number=A1234BC&prisoner_dob=1990-01-01")
        .headers(setAuthorisation(roles = listOf("ROLE_SEND_MONEY")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }
  }
}
