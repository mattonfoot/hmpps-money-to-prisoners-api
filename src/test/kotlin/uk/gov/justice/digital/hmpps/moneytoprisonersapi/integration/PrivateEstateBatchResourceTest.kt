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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import java.time.LocalDate

class PrivateEstateBatchResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @Autowired
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Autowired
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @BeforeEach
  fun setUp() {
    privateEstateBatchRepository.clearJoinTable()
    privateEstateBatchRepository.deleteAll()
    senderProfileRepository.deleteAll()
    prisonerProfileRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun createPrivatePrison(nomisId: String = "PRV"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Private Prison", region = "South")
    prison.privateEstate = true
    return prisonRepository.save(prison)
  }

  private fun createPublicPrison(nomisId: String = "PUB"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Public Prison", region = "North")
    prison.privateEstate = false
    return prisonRepository.save(prison)
  }

  private fun createAndSaveCredit(
    prison: String? = "PRV",
    resolution: CreditResolution = CreditResolution.PENDING,
  ): Credit {
    val credit = Credit(
      amount = 1000,
      prisonerNumber = "A1234BC",
      prisonerName = "John Smith",
      prison = prison,
      resolution = resolution,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  private fun createPrivateEstateBatch(
    ref: String,
    prison: Prison,
    date: LocalDate,
    credits: List<Credit> = emptyList(),
    totalAmount: Long = 0,
  ): PrivateEstateBatch {
    val batch = PrivateEstateBatch(
      ref = ref,
      prison = prison.nomisId,
      date = date,
      totalAmount = totalAmount,
    )
    val saved = privateEstateBatchRepository.save(batch)
    // Relationship is @OneToMany(mappedBy = "privateEstateBatch") on Credit;
    // ownership of the FK lives on the Credit side, so re-save each credit.
    credits.forEach { credit ->
      credit.privateEstateBatch = saved
      creditRepository.save(credit)
    }
    return saved
  }

  private fun createPrisonBankAccount(
    prison: Prison,
    postcode: String = "AB1 2CD",
    accountNumber: String = "12345678",
  ) {
    jdbcTemplate.update(
      """
      INSERT INTO prison_prisonbankaccount
        (address_line1, address_line2, city, postcode, sort_code, account_number, prison_id)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """.trimIndent(),
      "1 High Street",
      "Business Park",
      "Leeds",
      postcode,
      "112233",
      accountNumber,
      prison.nomisId,
    )
  }

  private fun createRemittanceEmail(prison: Prison, email: String) {
    jdbcTemplate.update(
      "INSERT INTO prison_remittanceemail (email, prison_id) VALUES (?, ?)",
      email,
      prison.nomisId,
    )
  }

  @Nested
  @DisplayName("GET /private-estate-batches/ (CRD-180 to CRD-182)")
  inner class ListPrivateEstateBatches {

    @Test
    @DisplayName("CRD-180 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated GET`() {
      webTestClient.get()
        .uri("/private-estate-batches/")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("CRD-180 - GET /private-estate-batches/ returns list")
    fun `should return list of private estate batches`() {
      val prison = createPrivatePrison()
      createPrisonBankAccount(prison)
      createRemittanceEmail(prison, "payments-1@example.com")
      createRemittanceEmail(prison, "payments-2@example.com")
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))

      webTestClient.get()
        .uri("/private-estate-batches/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(1)
        .jsonPath("$.results[0].date").isEqualTo("2024-03-15")
        .jsonPath("$.results[0].prison").isEqualTo("PRV")
        .jsonPath("$.results[0].total_amount").isEqualTo(0)
        .jsonPath("$.results[0].bank_account.postcode").isEqualTo("AB1 2CD")
        .jsonPath("$.results[0].bank_account.account_number").isEqualTo("12345678")
        .jsonPath("$.results[0].remittance_emails.length()").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-180 - GET /private-estate-batches/ returns 403 without ROLE_BANK_ADMIN")
    fun `should return 403 for non bank admin GET`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))

      webTestClient.get()
        .uri("/private-estate-batches/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("CRD-181 - Filter by date")
    fun `should filter by date`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))
      createPrivateEstateBatch("PRV/2024-03-16", prison, LocalDate.of(2024, 3, 16))

      webTestClient.get()
        .uri("/private-estate-batches/?date=2024-03-15")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].date").isEqualTo("2024-03-15")
    }

    @Test
    @DisplayName("CRD-181 - Filter by date__gte")
    fun `should filter by date__gte`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-14", prison, LocalDate.of(2024, 3, 14))
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))
      createPrivateEstateBatch("PRV/2024-03-16", prison, LocalDate.of(2024, 3, 16))

      webTestClient.get()
        .uri("/private-estate-batches/?date__gte=2024-03-15")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-181 - Filter by prison")
    fun `should filter by prison`() {
      // nomis_id is varchar(3) — keep ids to 3 characters.
      val prison1 = createPrivatePrison("PRV")
      val prison2 = createPrivatePrison("PRX")
      createPrivateEstateBatch("PRV/2024-03-15", prison1, LocalDate.of(2024, 3, 15))
      createPrivateEstateBatch("PRX/2024-03-15", prison2, LocalDate.of(2024, 3, 15))

      webTestClient.get()
        .uri("/private-estate-batches/?prison=PRV")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEqualTo("PRV")
    }
  }

  @Nested
  @DisplayName("GET /private-estate-batches/{ref}/ (CRD-183)")
  inner class GetPrivateEstateBatch {

    @Test
    @DisplayName("CRD-183 - GET /private-estate-batches/{prison}/{date}/ returns single batch")
    fun `should return single batch by ref`() {
      val prison = createPrivatePrison()
      createPrisonBankAccount(prison, postcode = "ZZ1 1ZZ", accountNumber = "87654321")
      createRemittanceEmail(prison, "remit@example.com")
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))

      webTestClient.get()
        .uri("/private-estate-batches/PRV/2024-03-15/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.date").isEqualTo("2024-03-15")
        .jsonPath("$.prison").isEqualTo("PRV")
        .jsonPath("$.bank_account.postcode").isEqualTo("ZZ1 1ZZ")
        .jsonPath("$.remittance_emails[0]").isEqualTo("remit@example.com")
    }

    @Test
    @DisplayName("CRD-183 - GET non-existent batch returns 404")
    fun `should return 404 for non-existent batch ref`() {
      webTestClient.get()
        .uri("/private-estate-batches/UNKNOWN/2024-03-15/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("PATCH /private-estate-batches/{prison}/{date}/ (CRD-184 to CRD-186)")
  inner class PatchPrivateEstateBatch {

    @Test
    @DisplayName("CRD-184 - PATCH with credited=true credits all credit_pending credits in batch and returns 204")
    fun `should credit all credit_pending credits in batch`() {
      val prison = createPrivatePrison()
      val credit1 = createAndSaveCredit(prison = "PRV", resolution = CreditResolution.PENDING)
      val credit2 = createAndSaveCredit(prison = "PRV", resolution = CreditResolution.PENDING)
      createPrivateEstateBatch(
        "PRV/2024-03-15",
        prison,
        LocalDate.of(2024, 3, 15),
        credits = listOf(credit1, credit2),
        totalAmount = 2000,
      )

      webTestClient.patch()
        .uri("/private-estate-batches/PRV/2024-03-15/")
        .headers(setAuthorisation(username = "bankadmin"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credited": true}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(credit1.id!!).get().resolution).isEqualTo(CreditResolution.CREDITED.value)
      assertThat(creditRepository.findById(credit2.id!!).get().resolution).isEqualTo(CreditResolution.CREDITED.value)
    }

    @Test
    @DisplayName("CRD-184b - PATCH without credited flag returns 400 and does not change resolution")
    fun `should return 400 when credited flag missing`() {
      val prison = createPrivatePrison()
      val credit = createAndSaveCredit(prison = "PRV", resolution = CreditResolution.PENDING)
      createPrivateEstateBatch(
        "PRV/2024-03-15",
        prison,
        LocalDate.of(2024, 3, 15),
        credits = listOf(credit),
      )

      webTestClient.patch()
        .uri("/private-estate-batches/PRV/2024-03-15/")
        .headers(setAuthorisation(username = "bankadmin"))
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .expectStatus().isBadRequest

      assertThat(creditRepository.findById(credit.id!!).get().resolution).isEqualTo(CreditResolution.PENDING.value)
    }

    @Test
    @DisplayName("CRD-184c - PUT returns 405 (only PATCH is permitted)")
    fun `should return 405 for PUT method`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))

      webTestClient.put()
        .uri("/private-estate-batches/PRV/2024-03-15/")
        .headers(setAuthorisation(username = "bankadmin"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credited": true}""")
        .exchange()
        .expectStatus().isEqualTo(405)
    }

    @Test
    @DisplayName("CRD-185 - PATCH non-existent batch returns 404")
    fun `should return 404 for non-existent batch on PATCH`() {
      webTestClient.patch()
        .uri("/private-estate-batches/UNKNOWN/2024-03-15/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credited": true}""")
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("GET /private-estate-batches/{prison}/{date}/credits/ (CRD-187 to CRD-189)")
  inner class GetPrivateEstateBatchCredits {

    @Test
    @DisplayName("CRD-187 - GET /private-estate-batches/{prison}/{date}/credits/ returns credits in batch")
    fun `should return credits for batch`() {
      val prison = createPrivatePrison()
      val credit1 = createAndSaveCredit(prison = "PRV")
      val credit2 = createAndSaveCredit(prison = "PRV")
      createPrivateEstateBatch(
        "PRV/2024-03-15",
        prison,
        LocalDate.of(2024, 3, 15),
        credits = listOf(credit1, credit2),
      )

      webTestClient.get()
        .uri("/private-estate-batches/PRV/2024-03-15/credits/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
        .jsonPath("$.results.length()").isEqualTo(2)
        .jsonPath("$.results[0].prison").isEqualTo("PRV")
    }

    @Test
    @DisplayName("CRD-187 - GET credits returns 403 without ROLE_BANK_ADMIN")
    fun `should return 403 for non bank admin batch credits`() {
      val prison = createPrivatePrison()
      createPrivateEstateBatch("PRV/2024-03-15", prison, LocalDate.of(2024, 3, 15))

      webTestClient.get()
        .uri("/private-estate-batches/PRV/2024-03-15/credits/")
        .headers(setAuthorisation(roles = listOf("ROLE_PRISON_CLERK")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @DisplayName("CRD-187 - GET credits for non-existent batch returns 404")
    fun `should return 404 for non-existent batch credits`() {
      webTestClient.get()
        .uri("/private-estate-batches/UNKNOWN/2024-03-15/credits/")
        .headers(setAuthorisation(roles = listOf("ROLE_BANK_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
    }
  }
}
