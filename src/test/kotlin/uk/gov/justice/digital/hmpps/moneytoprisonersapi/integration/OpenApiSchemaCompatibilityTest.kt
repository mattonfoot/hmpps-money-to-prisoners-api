package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * Verifies the Kotlin OpenAPI schema names match the Python API's schema names exactly,
 * so client SDKs generated from either spec produce identical type names.
 */
@DisplayName("OpenAPI Schema Compatibility")
class OpenApiSchemaCompatibilityTest : IntegrationTestBase() {

  /** Schema names that MUST appear in the Kotlin OpenAPI spec (matching Python). */
  private val expectedSchemaNames = setOf(
    // Core entity DTOs
    "AccountRequest",
    "Balance",
    "Batch",
    "BillingAddress",
    "Category",
    "CheckAutoAcceptRule",
    "CheckAutoAcceptRuleState",
    "Credit",
    "Credit Comment",
    "Detailed User",
    "Disbursement",
    "Disbursement Comment",
    "Event",
    "FileDownload",
    "Flag",
    "JobInformation",
    "MonitoredPartialEmailAddressSerialiser",
    "Notification",
    "Payment",
    "PerformanceData",
    "Population",
    "Prison Prison",
    "PrisonerAccountBalance",
    "PrisonerCreditNoticeEmail",
    "PrisonerLocation",
    "PrisonerProfile",
    "PrisonerValidity",
    "PrivateEstateBatch",
    "ProcessingBatch",
    "RecipientProfile",
    "Role",
    "SavedSearch",
    "SecurityCredit",
    "SenderProfile",
    "Transaction",
    // Strategy A: Direct request DTO renames matching Python
    "ChangePasswordWithCode",
    "ResetPassword",
    "CreateTransaction",
    "ReconcileTransaction",
    "DisbursementIds",
    "DisbursementConfirmation",
    "CreditedOnlyCredit",
    "IdsCredit",
    "UpdateRefundedTransaction",
    // Strategy B: Nested type extractions
    "BankTransferSenderDetails",
    "BankTransferRecipientDetails",
    "DebitCardSenderDetails",
    "SearchFilter",
    "Log",
    // Final gap closures — match every Python schema name
    "Basic User",
    "CheckCredit",
    "CreateNewPassword",
    "DebitCardSenderDetailsCardholderNames",
    "NOMIS Prison",
    "Null",
    "PrivateEstateBatchCredit",
    "ChangePassword",
  )

  @Test
  @DisplayName("schemas contain all expected Python-aligned names")
  fun `schemas match Python names`() {
    val schemaNames = fetchSchemaNames()
    val missing = expectedSchemaNames - schemaNames
    assertThat(missing).describedAs("Missing schema names from OpenAPI spec").isEmpty()
  }

  @Test
  @DisplayName("PaginatedResponse generic schemas are hidden from OpenAPI spec")
  fun `PaginatedResponse schemas are hidden`() {
    val schemaNames = fetchSchemaNames()
    val paginatedResponseSchemas = schemaNames.filter { it.startsWith("PaginatedResponse") }
    assertThat(paginatedResponseSchemas)
      .describedAs("PaginatedResponse* schemas should be hidden from OpenAPI spec")
      .isEmpty()
  }

  @Test
  @DisplayName("schema names do not have Dto suffix")
  fun `no Dto suffix in schema names`() {
    val schemaNames = fetchSchemaNames()
    val dtoSuffixed = schemaNames.filter { it.endsWith("Dto") }
    assertThat(dtoSuffixed)
      .describedAs("Schema names should not end with 'Dto'")
      .isEmpty()
  }

  @Test
  @DisplayName("private-estate-batches URLs use {ref} / {batch_ref} matching Python")
  fun `private estate batches URLs match Python`() {
    val paths = fetchPaths()
    assertThat(paths)
      .describedAs("/private-estate-batches/{ref}/ should appear in spec")
      .contains("/private-estate-batches/{ref}/")
    assertThat(paths)
      .describedAs("/private-estate-batches/{batch_ref}/credits/ should appear in spec")
      .contains("/private-estate-batches/{batch_ref}/credits/")
    val twoSegmentPaths = paths.filter {
      it.startsWith("/private-estate-batches/{prison}/{date}")
    }
    assertThat(twoSegmentPaths)
      .describedAs("/private-estate-batches should not expose {prison}/{date} two-segment paths")
      .isEmpty()
  }

  private fun fetchPaths(): Set<String> {
    val body = webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody(Map::class.java)
      .returnResult()
      .responseBody

    @Suppress("UNCHECKED_CAST")
    val paths = (body?.get("paths") as? Map<String, Any>) ?: emptyMap()
    return paths.keys
  }

  private fun fetchSchemaNames(): Set<String> {
    val body = webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody(Map::class.java)
      .returnResult()
      .responseBody

    @Suppress("UNCHECKED_CAST")
    val components = (body?.get("components") as? Map<String, Any>) ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    val schemas = (components["schemas"] as? Map<String, Any>) ?: emptyMap()
    return schemas.keys
  }
}
