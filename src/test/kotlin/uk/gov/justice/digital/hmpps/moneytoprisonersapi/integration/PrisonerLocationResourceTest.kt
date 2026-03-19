package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerLocation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerLocationRepository

class PrisonerLocationResourceTest : IntegrationTestBase() {

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

  private fun createLocation(
    prisonerNumber: String = "A1234BC",
    prison: Prison,
    active: Boolean = true,
    createdBy: String = "test_user",
  ): PrisonerLocation {
    val loc = PrisonerLocation(prisonerNumber = prisonerNumber, prison = prison, active = active, createdBy = createdBy)
    return prisonerLocationRepository.save(loc)
  }

  @Nested
  @DisplayName("POST /prisoner_locations/")
  inner class CreatePrisonerLocations {

    @Test
    @DisplayName("PRS-020 - Unauthenticated returns 401")
    fun `should return 401 for unauthenticated request`() {
      createPrison("LEI")
      webTestClient.post()
        .uri("/prisoner_locations/")
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-021 - Without ROLE_NOMS_OPS or ROLE_CASHBOOK returns 403")
    fun `should return 403 without required role`() {
      createPrison("LEI")
      webTestClient.post()
        .uri("/prisoner_locations/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PRS-022 - ROLE_NOMS_OPS can create locations")
    fun `should allow ROLE_NOMS_OPS to create locations`() {
      createPrison("LEI")
      webTestClient.post()
        .uri("/prisoner_locations/")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMS_OPS")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    @DisplayName("PRS-022 - ROLE_CASHBOOK can create locations")
    fun `should allow ROLE_CASHBOOK to create locations`() {
      createPrison("LEI")
      webTestClient.post()
        .uri("/prisoner_locations/")
        .headers(setAuthorisation(roles = listOf("ROLE_CASHBOOK")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    @DisplayName("PRS-023 - Dict input (single object not array) returns 400")
    fun `should return 400 when body is a single object not array`() {
      createPrison("LEI")
      webTestClient.post()
        .uri("/prisoner_locations/")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMS_OPS")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"prisoner_number": "A1234BC", "prison": "LEI"}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("PRS-024 - Invalid prison code returns 400")
    fun `should return 400 for invalid prison code`() {
      webTestClient.post()
        .uri("/prisoner_locations/")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMS_OPS")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "XXX"}]""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("PRS-025 - Creates new location with active=true")
    fun `should create location with active true`() {
      createPrison("LEI")

      webTestClient.post()
        .uri("/prisoner_locations/")
        .headers(setAuthorisation(username = "NOMS_USER", roles = listOf("ROLE_NOMS_OPS")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isCreated

      val locations = prisonerLocationRepository.findByPrisonerNumberAndActiveTrue("A1234BC")
      assertThat(locations).hasSize(1)
      assertThat(locations[0].active).isTrue()
      assertThat(locations[0].prisonerNumber).isEqualTo("A1234BC")
      assertThat(locations[0].createdBy).isEqualTo("NOMS_USER")
    }

    @Test
    @DisplayName("PRS-026 - Existing active location for same prisoner+prison deactivated")
    fun `should deactivate existing active location for same prisoner and prison`() {
      val prison = createPrison("LEI")
      val oldLocation = createLocation("A1234BC", prison, active = true)

      webTestClient.post()
        .uri("/prisoner_locations/")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMS_OPS")))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"prisoner_number": "A1234BC", "prison": "LEI"}]""")
        .exchange()
        .expectStatus().isCreated

      val refreshed = prisonerLocationRepository.findById(oldLocation.id!!).get()
      assertThat(refreshed.active).isFalse()
    }

    @Test
    @DisplayName("PRS-027 - Creates multiple locations from array")
    fun `should create multiple locations from array`() {
      createPrison("LEI")
      createPrison("MDI")

      webTestClient.post()
        .uri("/prisoner_locations/")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMS_OPS")))
        .header("Content-Type", "application/json")
        .bodyValue(
          """[
                        {"prisoner_number": "A1234BC", "prison": "LEI"},
                        {"prisoner_number": "B5678DE", "prison": "MDI"}
                    ]""",
        )
        .exchange()
        .expectStatus().isCreated

      assertThat(prisonerLocationRepository.findByPrisonerNumberAndActiveTrue("A1234BC")).hasSize(1)
      assertThat(prisonerLocationRepository.findByPrisonerNumberAndActiveTrue("B5678DE")).hasSize(1)
    }
  }

  @Nested
  @DisplayName("GET /prisoner_locations/{prisoner_number}/")
  inner class GetPrisonerLocation {

    @Test
    @DisplayName("PRS-028 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.get()
        .uri("/prisoner_locations/A1234BC/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-029 - Returns active location for prisoner")
    fun `should return active location for prisoner`() {
      val prison = createPrison("LEI")
      createLocation("A1234BC", prison, active = true)

      webTestClient.get()
        .uri("/prisoner_locations/A1234BC/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.prisoner_number").isEqualTo("A1234BC")
        .jsonPath("$.prison").isEqualTo("LEI")
        .jsonPath("$.active").isEqualTo(true)
    }

    @Test
    @DisplayName("PRS-030 - Returns 404 when no active location")
    fun `should return 404 when no active location found`() {
      webTestClient.get()
        .uri("/prisoner_locations/A9999XX/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("GET /prisoner_locations/can-upload/")
  inner class CanUpload {

    @Test
    @DisplayName("PRS-040 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.get()
        .uri("/prisoner_locations/can-upload/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-041 - Returns can_upload true when no recent deactivations")
    fun `should return can_upload true when no recent deactivations`() {
      webTestClient.get()
        .uri("/prisoner_locations/can-upload/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.can_upload").isEqualTo(true)
    }

    @Test
    @DisplayName("PRS-042 - Returns can_upload false when there are recently deactivated locations")
    fun `should return can_upload false when recently deactivated locations exist`() {
      val prison = createPrison("LEI")
      // Create an inactive location - its modified time will be set to now by @PrePersist
      // which is within the 10-minute window
      val loc = PrisonerLocation(prisonerNumber = "A1234BC", prison = prison, active = false, createdBy = "test")
      prisonerLocationRepository.save(loc)

      webTestClient.get()
        .uri("/prisoner_locations/can-upload/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.can_upload").isEqualTo(false)
    }
  }

  @Nested
  @DisplayName("POST /prisoner_locations/delete_old/")
  inner class DeleteOld {

    @Test
    @DisplayName("PRS-043 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.post()
        .uri("/prisoner_locations/delete_old/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-044 - Without ROLE_NOMS_OPS returns 403")
    fun `should return 403 without ROLE_NOMS_OPS`() {
      webTestClient.post()
        .uri("/prisoner_locations/delete_old/")
        .headers(setAuthorisation(roles = listOf("ROLE_CASHBOOK")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PRS-044 - Deactivates all active locations and returns 204")
    fun `should deactivate all active locations and return 204`() {
      val prison = createPrison("LEI")
      createLocation("A1234BC", prison, active = true)
      createLocation("B5678DE", prison, active = true)

      webTestClient.post()
        .uri("/prisoner_locations/delete_old/")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMS_OPS")))
        .exchange()
        .expectStatus().isNoContent

      val allLocations = prisonerLocationRepository.findAll()
      assertThat(allLocations).allMatch { !it.active }
    }
  }

  @Nested
  @DisplayName("POST /prisoner_locations/delete_inactive/")
  inner class DeleteInactive {

    @Test
    @DisplayName("PRS-046 - Returns 401 without authentication")
    fun `should return 401 without authentication`() {
      webTestClient.post()
        .uri("/prisoner_locations/delete_inactive/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("PRS-047 - Without ROLE_NOMS_OPS returns 403")
    fun `should return 403 without ROLE_NOMS_OPS`() {
      webTestClient.post()
        .uri("/prisoner_locations/delete_inactive/")
        .headers(setAuthorisation(roles = listOf("ROLE_CASHBOOK")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("PRS-047 - Deletes all inactive locations and returns 204")
    fun `should delete all inactive locations and return 204`() {
      val prison = createPrison("LEI")
      createLocation("A1234BC", prison, active = true)
      createLocation("B5678DE", prison, active = false)
      createLocation("C9012FG", prison, active = false)

      webTestClient.post()
        .uri("/prisoner_locations/delete_inactive/")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMS_OPS")))
        .exchange()
        .expectStatus().isNoContent

      val remaining = prisonerLocationRepository.findAll()
      assertThat(remaining).hasSize(1)
      assertThat(remaining[0].prisonerNumber).isEqualTo("A1234BC")
    }
  }
}
