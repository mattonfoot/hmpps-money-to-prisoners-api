package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPopulation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerLocation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonCategoryRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonPopulationRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerLocationRepository

class PrisonResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var prisonCategoryRepository: PrisonCategoryRepository

  @Autowired
  private lateinit var prisonPopulationRepository: PrisonPopulationRepository

  @Autowired
  private lateinit var prisonerLocationRepository: PrisonerLocationRepository

  @BeforeEach
  fun setUp() {
    prisonerLocationRepository.deleteAll()
    prisonRepository.deleteAll()
    prisonCategoryRepository.deleteAll()
    prisonPopulationRepository.deleteAll()
  }

  private fun createPrison(nomisId: String, name: String = "Test Prison", privateEstate: Boolean = false): Prison {
    val prison = Prison(nomisId = nomisId, name = name, region = "Test Region", privateEstate = privateEstate)
    return prisonRepository.save(prison)
  }

  private fun createPrisonerLocation(prisonerNumber: String, prison: Prison, active: Boolean = true): PrisonerLocation {
    val loc = PrisonerLocation(prisonerNumber = prisonerNumber, prison = prison, active = active, createdBy = "test_user")
    return prisonerLocationRepository.save(loc)
  }

  @Nested
  @DisplayName("GET /prisons/")
  inner class ListPrisons {

    @Test
    @DisplayName("PRS-001 - GET /prisons/ returns 200 without authentication")
    fun `should return 200 without authentication`() {
      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus()
        .isOk
    }

    @Test
    @DisplayName("PRS-002 - Returns paginated list of prisons")
    fun `should return paginated list of prisons`() {
      createPrison("LEI", "Leeds")
      createPrison("MDI", "Moorland")

      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(2)
    }

    @Test
    @DisplayName("PRS-003 - Returns empty list when no prisons exist")
    fun `should return empty list when no prisons`() {
      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(0)
    }

    @Test
    @DisplayName("PRS-004 - Prison response includes expected fields")
    fun `should return prison with expected fields`() {
      createPrison("LEI", "HMP Leeds")

      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].nomis_id").isEqualTo("LEI")
        .jsonPath("$.results[0].name").isEqualTo("HMP Leeds")
        .jsonPath("$.results[0].short_name").isEqualTo("Leeds")
        .jsonPath("$.results[0].pre_approval_required").isEqualTo(false)
        .jsonPath("$.results[0].private_estate").isEqualTo(false)
        .jsonPath("$.results[0].use_nomis_for_balances").isEqualTo(true)
    }

    @Test
    @DisplayName("PRS-005 - short_name strips HMP prefix")
    fun `should strip HMP prefix for short_name`() {
      createPrison("LEI", "HMP Leeds")

      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].short_name").isEqualTo("Leeds")
    }

    @Test
    @DisplayName("PRS-006 - short_name strips YOI prefix")
    fun `should strip YOI prefix for short_name`() {
      createPrison("WMI", "YOI Wetherby")

      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].short_name").isEqualTo("Wetherby")
    }

    @Test
    @DisplayName("PRS-007 - short_name strips STC prefix")
    fun `should strip STC prefix for short_name`() {
      createPrison("STC1", "STC Some Place")

      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].short_name").isEqualTo("Some Place")
    }

    @Test
    @DisplayName("PRS-008 - short_name strips IRC prefix")
    fun `should strip IRC prefix for short_name`() {
      createPrison("IRC1", "IRC Some Place")

      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].short_name").isEqualTo("Some Place")
    }

    @Test
    @DisplayName("PRS-009 - short_name strips HMYOI prefix")
    fun `should strip HMYOI prefix for short_name`() {
      createPrison("HYI", "HMYOI Wetherby")

      webTestClient.get()
        .uri("/prisons/")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].short_name").isEqualTo("Wetherby")
    }

    @Test
    @DisplayName("PRS-010 - exclude_empty_prisons filter excludes prisons with no active locations")
    fun `should exclude prisons with no active locations when exclude_empty_prisons is true`() {
      val prison1 = createPrison("LEI", "HMP Leeds")
      createPrison("MDI", "HMP Moorland")
      createPrisonerLocation("A1234BC", prison1, active = true)

      webTestClient.get()
        .uri("/prisons/?exclude_empty_prisons=true")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].nomis_id").isEqualTo("LEI")
    }

    @Test
    @DisplayName("PRS-011 - without exclude_empty_prisons all prisons returned")
    fun `should return all prisons when exclude_empty_prisons is false`() {
      val prison1 = createPrison("LEI", "HMP Leeds")
      createPrison("MDI", "HMP Moorland")
      createPrisonerLocation("A1234BC", prison1, active = true)

      webTestClient.get()
        .uri("/prisons/?exclude_empty_prisons=false")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("PRS-012 - exclude_empty_prisons excludes prisons with only inactive locations")
    fun `should exclude prisons with only inactive locations`() {
      val prison1 = createPrison("LEI", "HMP Leeds")
      createPrisonerLocation("A1234BC", prison1, active = false)

      webTestClient.get()
        .uri("/prisons/?exclude_empty_prisons=true")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("GET /prison_populations/")
  inner class ListPrisonPopulations {

    @Test
    @DisplayName("PRS-080 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.get()
        .uri("/prison_populations/")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    @DisplayName("PRS-080 - Returns paginated list of populations")
    fun `should return paginated list of populations`() {
      prisonPopulationRepository.save(PrisonPopulation(name = "Adult"))
      prisonPopulationRepository.save(PrisonPopulation(name = "Youth"))

      webTestClient.get()
        .uri("/prison_populations/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results").isArray
        .jsonPath("$.results[0].name").exists()
    }
  }

  @Nested
  @DisplayName("GET /prison_categories/")
  inner class ListPrisonCategories {

    @Test
    @DisplayName("PRS-082 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.get()
        .uri("/prison_categories/")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    @DisplayName("PRS-082 - Returns paginated list of categories")
    fun `should return paginated list of categories`() {
      prisonCategoryRepository.save(PrisonCategory(name = "Category A"))
      prisonCategoryRepository.save(PrisonCategory(name = "Category B"))

      webTestClient.get()
        .uri("/prison_categories/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results").isArray
        .jsonPath("$.results[0].name").exists()
    }
  }
}
