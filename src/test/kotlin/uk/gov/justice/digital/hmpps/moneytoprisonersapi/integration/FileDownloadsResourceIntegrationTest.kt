package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FileDownloadsResourceIntegrationTest : IntegrationTestBase() {

  @Test
  @DisplayName("COR-001 - GET /file-downloads/ returns 405")
  fun `should return method not allowed for GET file downloads`() {
    webTestClient.get()
      .uri("/file-downloads/")
      .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
      .exchange()
      .expectStatus()
      .isEqualTo(405)
  }

  @Test
  @DisplayName("COR-001 - OpenAPI exposes POST but not GET for /file-downloads/")
  fun `should expose post only for file downloads collection in api docs`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paths['/file-downloads/'].post").exists()
      .jsonPath("$.paths['/file-downloads/'].get").doesNotExist()
  }
}
