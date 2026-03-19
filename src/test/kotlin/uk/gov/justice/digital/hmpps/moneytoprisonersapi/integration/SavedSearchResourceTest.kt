package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SavedSearch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SavedSearchRepository

class SavedSearchResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: SavedSearchRepository

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Nested
  @DisplayName("GET /security/searches/ (SEC-122)")
  inner class ListSearches {

    @Test
    @DisplayName("SEC-122 - Returns 401 for unauthenticated request")
    fun `should return 401 for unauthenticated`() {
      webTestClient.get()
        .uri("/security/searches/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("SEC-122 - Returns only user's own searches")
    fun `should return only current user's searches`() {
      repository.save(SavedSearch(username = "user1", description = "My search", endpoint = "/credits/", filters = null))
      repository.save(SavedSearch(username = "user2", description = "Other search", endpoint = "/credits/", filters = null))

      webTestClient.get()
        .uri("/security/searches/")
        .headers(setAuthorisation(username = "user1"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.length()").isEqualTo(1)
        .jsonPath("$[0].description").isEqualTo("My search")
    }
  }

  @Nested
  @DisplayName("POST /security/searches/ (SEC-120 to SEC-121)")
  inner class CreateSearch {

    @Test
    @DisplayName("SEC-120 - Creates a saved search")
    fun `should create a saved search`() {
      webTestClient.post()
        .uri("/security/searches/")
        .headers(setAuthorisation(username = "user1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"description": "My search", "endpoint": "/credits/", "filters": null}""")
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.description").isEqualTo("My search")
        .jsonPath("$.endpoint").isEqualTo("/credits/")

      assertThat(repository.findByUsername("user1")).hasSize(1)
    }
  }

  @Nested
  @DisplayName("PATCH /security/searches/{id}/ (SEC-123)")
  inner class UpdateSearch {

    @Test
    @DisplayName("SEC-123 - Updates a saved search")
    fun `should update a saved search`() {
      val search = repository.save(SavedSearch(username = "user1", description = "Old", endpoint = "/credits/", filters = null))

      webTestClient.patch()
        .uri("/security/searches/${search.id}/")
        .headers(setAuthorisation(username = "user1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"description": "Updated"}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.description").isEqualTo("Updated")
    }

    @Test
    @DisplayName("SEC-123 - Returns 404 if search belongs to different user")
    fun `should return 404 if search belongs to different user`() {
      val search = repository.save(SavedSearch(username = "user2", description = "Other", endpoint = "/credits/", filters = null))

      webTestClient.patch()
        .uri("/security/searches/${search.id}/")
        .headers(setAuthorisation(username = "user1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"description": "Hacked"}""")
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("DELETE /security/searches/{id}/ (SEC-124)")
  inner class DeleteSearch {

    @Test
    @DisplayName("SEC-124 - Deletes a saved search")
    fun `should delete a saved search`() {
      val search = repository.save(SavedSearch(username = "user1", description = "My search", endpoint = "/credits/", filters = null))

      webTestClient.delete()
        .uri("/security/searches/${search.id}/")
        .headers(setAuthorisation(username = "user1"))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.count()).isEqualTo(0L)
    }

    @Test
    @DisplayName("SEC-124 - Returns 404 if search belongs to different user")
    fun `should return 404 if search belongs to different user`() {
      val search = repository.save(SavedSearch(username = "user2", description = "Other", endpoint = "/credits/", filters = null))

      webTestClient.delete()
        .uri("/security/searches/${search.id}/")
        .headers(setAuthorisation(username = "user1"))
        .exchange()
        .expectStatus().isNotFound
    }
  }
}
