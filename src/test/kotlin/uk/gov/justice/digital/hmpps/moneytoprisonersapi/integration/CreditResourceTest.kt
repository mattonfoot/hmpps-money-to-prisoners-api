package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.BillingAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPopulation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BillingAddressRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonCategoryRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonPopulationRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SecurityCheckRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.TransactionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CreditResourceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var prisonCategoryRepository: PrisonCategoryRepository

  @Autowired
  private lateinit var prisonPopulationRepository: PrisonPopulationRepository

  @Autowired
  private lateinit var logRepository: LogRepository

  @Autowired
  private lateinit var securityCheckRepository: SecurityCheckRepository

  @Autowired
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Autowired
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  @Autowired
  private lateinit var paymentRepository: PaymentRepository

  @Autowired
  private lateinit var billingAddressRepository: BillingAddressRepository

  @Autowired
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @BeforeEach
  fun setUp() {
    privateEstateBatchRepository.deleteAll()
    senderProfileRepository.deleteAll()
    prisonerProfileRepository.deleteAll()
    logRepository.deleteAll()
    securityCheckRepository.deleteAll()
    paymentRepository.deleteAll()
    transactionRepository.deleteAll()
    creditRepository.deleteAll()
    billingAddressRepository.deleteAll()
    prisonRepository.deleteAll()
    prisonCategoryRepository.deleteAll()
    prisonPopulationRepository.deleteAll()
  }

  private fun createAndSaveTransaction(
    credit: Credit,
    senderName: String? = "Alice Johnson",
    senderSortCode: String? = "112233",
    senderAccountNumber: String? = "12345678",
    senderRollNumber: String? = null,
  ): Transaction {
    val transaction = Transaction(
      amount = credit.amount,
      senderName = senderName,
      senderSortCode = senderSortCode,
      senderAccountNumber = senderAccountNumber,
      senderRollNumber = senderRollNumber,
    )
    transaction.credit = credit
    credit.source = CreditSource.BANK_TRANSFER
    creditRepository.save(credit)
    return transactionRepository.save(transaction)
  }

  private fun createAndSavePayment(
    credit: Credit,
    cardholderName: String? = "Bob Cardholder",
    email: String? = "bob@example.com",
    ipAddress: String? = "192.168.1.1",
    cardNumberFirstDigits: String? = "411111",
    cardNumberLastDigits: String? = "1234",
    cardExpiryDate: String? = "12/25",
    billingAddress: BillingAddress? = null,
    uuid: UUID = UUID.randomUUID(),
  ): Payment {
    val payment = Payment(
      uuid = uuid,
      amount = credit.amount,
      cardholderName = cardholderName,
      email = email,
      ipAddress = ipAddress,
      cardNumberFirstDigits = cardNumberFirstDigits,
      cardNumberLastDigits = cardNumberLastDigits,
      cardExpiryDate = cardExpiryDate,
    )
    payment.credit = credit
    payment.billingAddress = billingAddress
    credit.source = CreditSource.ONLINE
    creditRepository.save(credit)
    return paymentRepository.save(payment)
  }

  private fun createAndSaveBillingAddress(postcode: String? = "SW1A 1AA"): BillingAddress {
    val address = BillingAddress(postcode = postcode)
    return billingAddressRepository.save(address)
  }

  private fun createAndSavePrison(
    nomisId: String,
    name: String = "",
    region: String = "",
    categories: Set<PrisonCategory> = emptySet(),
    populations: Set<PrisonPopulation> = emptySet(),
  ): Prison {
    val prison = Prison(nomisId = nomisId, name = name, region = region)
    prison.categories = categories.toMutableSet()
    prison.populations = populations.toMutableSet()
    return prisonRepository.save(prison)
  }

  private fun createAndSaveCredit(
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prisonerName: String? = "John Smith",
    prisonerDob: LocalDate? = LocalDate.of(1990, 1, 15),
    prison: String? = "LEI",
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    reviewed: Boolean = false,
    reconciled: Boolean = false,
    receivedAt: LocalDateTime? = LocalDateTime.of(2024, 3, 15, 10, 0),
    owner: String? = null,
    incompleteSenderInfo: Boolean = false,
    source: CreditSource = CreditSource.BANK_TRANSFER,
  ): Credit {
    if (prison != null && !prisonRepository.existsById(prison)) {
      createAndSavePrison(nomisId = prison)
    }
    val credit = Credit(
      amount = amount,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      prisonerDob = prisonerDob,
      prison = prison,
      resolution = resolution,
      blocked = blocked,
      reviewed = reviewed,
      reconciled = reconciled,
      receivedAt = receivedAt,
      owner = owner,
      incompleteSenderInfo = incompleteSenderInfo,
    )
    credit.source = source
    return creditRepository.save(credit)
  }

  @Nested
  @DisplayName("GET /credits/")
  inner class ListCredits {

    @Test
    @DisplayName("CRD-021 - Unauthenticated request returns 401")
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/credits/")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    @DisplayName("CRD-020 - GET /credits/ returns 200 with paginated response")
    fun `should return paginated response format`() {
      createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")

      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.next").isEmpty
        .jsonPath("$.previous").isEmpty
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-020 - Empty database returns empty results")
    fun `should return empty results when no credits exist`() {
      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
        .jsonPath("$.results").isArray
        .jsonPath("$.results.length()").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-020 - Response includes all credit fields")
    fun `should include all credit fields in response`() {
      createAndSaveCredit(
        amount = 5000,
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        prison = "LEI",
        resolution = CreditResolution.PENDING,
        receivedAt = LocalDateTime.of(2024, 3, 15, 10, 30),
      )

      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.results[0].id").isNotEmpty
        .jsonPath("$.results[0].amount").isEqualTo(5000)
        .jsonPath("$.results[0].prisoner_number").isEqualTo("A1234BC")
        .jsonPath("$.results[0].prisoner_name").isEqualTo("John Smith")
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
        .jsonPath("$.results[0].resolution").isEqualTo("PENDING")
        .jsonPath("$.results[0].source").isEqualTo("BANK_TRANSFER")
        .jsonPath("$.results[0].status").isEqualTo("credit_pending")
        .jsonPath("$.results[0].blocked").isEqualTo(false)
        .jsonPath("$.results[0].reviewed").isEqualTo(false)
        .jsonPath("$.results[0].reconciled").isEqualTo(false)
        .jsonPath("$.results[0].received_at").isNotEmpty
        .jsonPath("$.results[0].created").isNotEmpty
        .jsonPath("$.results[0].modified").isNotEmpty
    }

    @Test
    @DisplayName("CRD-010 - Excludes initial and failed credits")
    fun `should exclude initial and failed credits`() {
      createAndSaveCredit(resolution = CreditResolution.INITIAL)
      createAndSaveCredit(resolution = CreditResolution.FAILED)
      createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Status (CRD-030 to CRD-037)")
  inner class StatusFilters {

    @Test
    @DisplayName("CRD-030 - Filter status=credit_pending")
    fun `should filter by status credit_pending`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=CREDIT_PENDING")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("credit_pending")
    }

    @Test
    @DisplayName("CRD-031 - Filter status=credited")
    fun `should filter by status credited`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=CREDITED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("credited")
    }

    @Test
    @DisplayName("CRD-032 - Filter status=refund_pending")
    fun `should filter by status refund_pending`() {
      createAndSaveCredit(prison = null, resolution = CreditResolution.PENDING, blocked = false, incompleteSenderInfo = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=REFUND_PENDING")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("refund_pending")
    }

    @Test
    @DisplayName("CRD-033 - Filter status=refunded")
    fun `should filter by status refunded`() {
      createAndSaveCredit(resolution = CreditResolution.REFUNDED)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=REFUNDED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("refunded")
    }

    @Test
    @DisplayName("CRD-034 - Filter status=failed")
    fun `should filter by status failed`() {
      createAndSaveCredit(resolution = CreditResolution.FAILED)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=FAILED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].status").isEqualTo("failed")
    }

    @Test
    @DisplayName("CRD-035 - Invalid status returns empty set")
    fun `should return empty for status with no matches`() {
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?status=FAILED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-036 - Filter valid=true returns credit_pending or credited")
    fun `should filter valid true`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prison = null, resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?valid=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-037 - Filter valid=false returns non-valid credits")
    fun `should filter valid false`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      createAndSaveCredit(resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prison = null, resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?valid=false")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Prison (CRD-040 to CRD-046)")
  inner class PrisonFilters {

    @Test
    @DisplayName("CRD-040 - Filter prison={nomis_id}")
    fun `should filter by exact prison id`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "MDI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison=LEI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
    }

    @Test
    @DisplayName("CRD-042 - Filter prison__isnull=True")
    fun `should filter credits with no prison`() {
      createAndSaveCredit(prison = null, resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison__isnull=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEmpty
    }

    @Test
    @DisplayName("CRD-046 - Invalid prison ID returns empty set")
    fun `should return empty for non-existent prison`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison=NONEXISTENT")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-041 - Filter prison={id1}&prison={id2} multiple prison IDs")
    fun `should filter by multiple prison IDs`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "MDI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "BXI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison=LEI&prison=MDI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-041 - Single value in prison list works as exact match")
    fun `should filter by single prison in list`() {
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "MDI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison=LEI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prison").isEqualTo("LEI")
    }

    @Test
    @DisplayName("CRD-043 - Filter prison_region case-insensitive substring")
    fun `should filter by prison region`() {
      val leiPrison = createAndSavePrison(nomisId = "LEI", name = "Leeds", region = "Yorkshire and Humber")
      val mdiPrison = createAndSavePrison(nomisId = "MDI", name = "Moorland", region = "Yorkshire and Humber")
      createAndSavePrison(nomisId = "BXI", name = "Brixton", region = "London")
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "MDI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "BXI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison_region=Yorkshire")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-043 - Prison region filter is case-insensitive")
    fun `should filter by prison region case-insensitively`() {
      createAndSavePrison(nomisId = "LEI", region = "Yorkshire and Humber")
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison_region=yorkshire")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-043 - Non-matching region returns empty set")
    fun `should return empty for non-matching region`() {
      createAndSavePrison(nomisId = "LEI", region = "Yorkshire and Humber")
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison_region=Scotland")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-044 - Filter prison_category matches any category")
    fun `should filter by prison category`() {
      val catB = prisonCategoryRepository.save(PrisonCategory(name = "Category B"))
      val catC = prisonCategoryRepository.save(PrisonCategory(name = "Category C"))
      createAndSavePrison(nomisId = "LEI", categories = setOf(catB))
      createAndSavePrison(nomisId = "MDI", categories = setOf(catC))
      createAndSavePrison(nomisId = "BXI", categories = setOf(catB, catC))
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "MDI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "BXI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison_category=Category B")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-044 - Non-matching category returns empty set")
    fun `should return empty for non-matching category`() {
      val catB = prisonCategoryRepository.save(PrisonCategory(name = "Category B"))
      createAndSavePrison(nomisId = "LEI", categories = setOf(catB))
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison_category=Category A")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-045 - Filter prison_population matches any population")
    fun `should filter by prison population`() {
      val adult = prisonPopulationRepository.save(PrisonPopulation(name = "Adult"))
      val young = prisonPopulationRepository.save(PrisonPopulation(name = "Young Offender"))
      createAndSavePrison(nomisId = "LEI", populations = setOf(adult))
      createAndSavePrison(nomisId = "MDI", populations = setOf(adult, young))
      createAndSavePrison(nomisId = "BXI", populations = setOf(young))
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "MDI", resolution = CreditResolution.PENDING)
      createAndSaveCredit(prison = "BXI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison_population=Adult")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-045 - Non-matching population returns empty set")
    fun `should return empty for non-matching population`() {
      val adult = prisonPopulationRepository.save(PrisonPopulation(name = "Adult"))
      createAndSavePrison(nomisId = "LEI", populations = setOf(adult))
      createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING)

      webTestClient.get()
        .uri("/credits/?prison_population=Juvenile")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Amount (CRD-050 to CRD-057)")
  inner class AmountFilters {

    @Test
    @DisplayName("CRD-050 - Filter amount={exact}")
    fun `should filter by exact amount`() {
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2000, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount=1000")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(1000)
    }

    @Test
    @DisplayName("CRD-051 - Filter amount__gte")
    fun `should filter by minimum amount`() {
      createAndSaveCredit(amount = 500, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount__gte=1000")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-052 - Filter amount__lte")
    fun `should filter by maximum amount`() {
      createAndSaveCredit(amount = 500, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount__lte=1000")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-053 - Filter amount__endswith")
    fun `should filter by amount endswith`() {
      createAndSaveCredit(amount = 1050, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2050, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1099, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount__endswith=50")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-053 - amount__endswith with no matches returns empty")
    fun `should return empty for amount endswith with no matches`() {
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount__endswith=99")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(0)
    }

    @Test
    @DisplayName("CRD-054 - Filter amount__regex")
    fun `should filter by amount regex`() {
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri { it.path("/credits/").queryParam("amount__regex", "^1.*").build() }
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-055 - Filter exclude_amount__endswith")
    fun `should exclude by amount endswith`() {
      createAndSaveCredit(amount = 1050, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2050, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1099, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?exclude_amount__endswith=50")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(1099)
    }

    @Test
    @DisplayName("CRD-056 - Filter exclude_amount__regex")
    fun `should exclude by amount regex`() {
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri { it.path("/credits/").queryParam("exclude_amount__regex", "^1.*").build() }
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(2000)
    }

    @Test
    @DisplayName("CRD-057 - Multiple amount filters combine with AND")
    fun `should combine amount filters with AND`() {
      createAndSaveCredit(amount = 500, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?amount__gte=800&amount__lte=1200")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(1000)
    }

    @Test
    @DisplayName("CRD-057 - endswith and regex combine with AND")
    fun `should combine endswith and regex filters with AND`() {
      createAndSaveCredit(amount = 1050, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2050, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1099, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri { it.path("/credits/").queryParam("amount__endswith", "50").queryParam("amount__regex", "^1.*").build() }
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].amount").isEqualTo(1050)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Other (CRD-080 to CRD-085)")
  inner class OtherFilters {

    @Test
    @DisplayName("CRD-080 - Filter prisoner_name case-insensitive substring")
    fun `should filter by prisoner name`() {
      createAndSaveCredit(prisonerName = "John Smith", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerName = "Jane Doe", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?prisoner_name=john")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_name").isEqualTo("John Smith")
    }

    @Test
    @DisplayName("CRD-081 - Filter prisoner_number exact match")
    fun `should filter by prisoner number`() {
      createAndSaveCredit(prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerNumber = "B5678DE", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?prisoner_number=A1234BC")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_number").isEqualTo("A1234BC")
    }

    @Test
    @DisplayName("CRD-082 - Filter by user (owner)")
    fun `should filter by owner`() {
      createAndSaveCredit(owner = "clerk1", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(owner = "clerk2", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?user=clerk1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-083 - Filter by resolution")
    fun `should filter by resolution`() {
      createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")
      createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?resolution=CREDITED")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].resolution").isEqualTo("CREDITED")
    }

    @Test
    @DisplayName("CRD-084 - Filter reviewed=true")
    fun `should filter by reviewed flag`() {
      createAndSaveCredit(reviewed = true, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(reviewed = false, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?reviewed=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].reviewed").isEqualTo(true)
    }

    @Test
    @DisplayName("CRD-085 - Filter received_at__gte/lt datetime range")
    fun `should filter by received_at range`() {
      createAndSaveCredit(receivedAt = LocalDateTime.of(2024, 1, 1, 10, 0), resolution = CreditResolution.CREDITED)
      createAndSaveCredit(receivedAt = LocalDateTime.of(2024, 2, 15, 10, 0), resolution = CreditResolution.CREDITED)
      createAndSaveCredit(receivedAt = LocalDateTime.of(2024, 3, 30, 10, 0), resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?received_at__gte=2024-02-01T00:00:00&received_at__lt=2024-03-01T00:00:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Other (CRD-086 to CRD-091)")
  inner class CreditListFiltersOtherExtended {

    @Test
    @DisplayName("CRD-086 - Filter logged_at__gte truncated to UTC date")
    fun `should filter by logged_at__gte`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      logRepository.save(Log(action = LogAction.CREDITED, credit = credit1).also { it.created = LocalDateTime.of(2024, 3, 14, 23, 59) })
      logRepository.save(Log(action = LogAction.CREDITED, credit = credit2).also { it.created = LocalDateTime.of(2024, 3, 15, 10, 0) })

      webTestClient.get()
        .uri("/credits/?logged_at__gte=2024-03-15T00:00:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-086 - Filter logged_at__lt truncated to UTC date")
    fun `should filter by logged_at__lt`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      logRepository.save(Log(action = LogAction.CREDITED, credit = credit1).also { it.created = LocalDateTime.of(2024, 3, 14, 23, 59) })
      logRepository.save(Log(action = LogAction.CREDITED, credit = credit2).also { it.created = LocalDateTime.of(2024, 3, 15, 10, 0) })

      webTestClient.get()
        .uri("/credits/?logged_at__lt=2024-03-15T00:00:00")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-087 - Filter security_check__isnull=true returns credits without security check")
    fun `should filter security_check__isnull true`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      securityCheckRepository.save(SecurityCheck(credit = credit2))

      webTestClient.get()
        .uri("/credits/?security_check__isnull=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-087 - Filter security_check__isnull=false returns credits with security check")
    fun `should filter security_check__isnull false`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      securityCheckRepository.save(SecurityCheck(credit = credit2))

      webTestClient.get()
        .uri("/credits/?security_check__isnull=false")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-088 - Filter security_check__actioned_by__isnull=true returns unactioned checks")
    fun `should filter security_check actioned_by isnull true`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      securityCheckRepository.save(SecurityCheck(credit = credit1, actionedBy = null))
      securityCheckRepository.save(SecurityCheck(credit = credit2, actionedBy = "admin1"))

      webTestClient.get()
        .uri("/credits/?security_check__actioned_by__isnull=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-089 - Filter exclude_credit__in excludes specific credit IDs")
    fun `should exclude specific credit IDs`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit3 = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?exclude_credit__in=${credit1.id}&exclude_credit__in=${credit3.id}")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].id").isEqualTo(credit2.id!!.toInt())
    }

    @Test
    @DisplayName("CRD-090 - Filter monitored=true returns credits linked to monitored profiles")
    fun `should filter monitored credits`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit3 = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      val senderProfile = SenderProfile()
      senderProfile.credits.add(credit1)
      senderProfile.monitoringUsers.add("user1")
      senderProfileRepository.save(senderProfile)

      val prisonerProfile = PrisonerProfile(prisonerNumber = "A1234BC")
      prisonerProfile.credits.add(credit2)
      prisonerProfile.monitoringUsers.add("user2")
      prisonerProfileRepository.save(prisonerProfile)

      webTestClient.get()
        .uri("/credits/?monitored=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }

    @Test
    @DisplayName("CRD-091 - Filter pk={id1,id2} returns specific credits")
    fun `should filter by pk`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val credit3 = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?pk=${credit1.id}&pk=${credit3.id}")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("POST /credits/actions/credit/ (CRD-110 to CRD-119)")
  inner class CreditPrisonersAction {

    @Test
    @DisplayName("CRD-118 - returns 204 when all credits are processed with no conflicts")
    fun `CRD-118 should return 204 when all credits are processed successfully`() {
      val credit = createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)

      webTestClient.post()
        .uri("/credits/actions/credit/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"id": ${credit.id}, "credited": true}]""")
        .exchange()
        .expectStatus()
        .isNoContent
    }

    @Test
    @DisplayName("CRD-113 - sets resolution=CREDITED, owner, and nomis_transaction_id on credit")
    fun `CRD-113 should update credit to CREDITED state with owner and nomis_transaction_id`() {
      val credit = createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)

      webTestClient.post()
        .uri("/credits/actions/credit/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"id": ${credit.id}, "credited": true, "nomis_transaction_id": "TX-001"}]""")
        .exchange()
        .expectStatus()
        .isNoContent

      val updated = creditRepository.findById(credit.id!!).get()
      assertThat(updated.resolution).isEqualTo(CreditResolution.CREDITED)
      assertThat(updated.owner).isEqualTo("clerk1")
      assertThat(updated.nomisTransactionId).isEqualTo("TX-001")
    }

    @Test
    @DisplayName("CRD-114 - creates a CREDITED log entry with user reference")
    fun `CRD-114 should create a log entry with LogAction CREDITED`() {
      val credit = createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)

      webTestClient.post()
        .uri("/credits/actions/credit/")
        .headers(setAuthorisation(username = "clerk1"))
        .header("Content-Type", "application/json")
        .bodyValue("""[{"id": ${credit.id}, "credited": true}]""")
        .exchange()
        .expectStatus()
        .isNoContent

      val logs = logRepository.findAll().filter { it.credit?.id == credit.id && it.action == LogAction.CREDITED }
      assertThat(logs).hasSize(1)
      assertThat(logs[0].userId).isEqualTo("clerk1")
    }

    @Test
    @DisplayName("CRD-112 - credits not in credit_pending state are returned as conflict_ids with HTTP 200")
    fun `CRD-112 should return 200 with conflict_ids for non-credit_pending credits`() {
      val alreadyCredited = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.post()
        .uri("/credits/actions/credit/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"id": ${alreadyCredited.id}, "credited": true}]""")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.conflict_ids").isArray
        .jsonPath("$.conflict_ids[0]").isEqualTo(alreadyCredited.id!!.toInt())
    }

    @Test
    @DisplayName("CRD-112 - blocked credits are returned as conflict_ids")
    fun `CRD-112 blocked credits are returned as conflict_ids`() {
      val blockedCredit = createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = true)

      webTestClient.post()
        .uri("/credits/actions/credit/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"id": ${blockedCredit.id}, "credited": true}]""")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.conflict_ids[0]").isEqualTo(blockedCredit.id!!.toInt())
    }

    @Test
    @DisplayName("CRD-112 - mix of valid and invalid: valid credited, invalid in conflict_ids")
    fun `CRD-112 processes valid credits and returns invalid as conflict_ids`() {
      val validCredit = createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      val invalidCredit = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.post()
        .uri("/credits/actions/credit/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"id": ${validCredit.id}, "credited": true}, {"id": ${invalidCredit.id}, "credited": true}]""")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.conflict_ids.length()").isEqualTo(1)
        .jsonPath("$.conflict_ids[0]").isEqualTo(invalidCredit.id!!.toInt())

      val updatedValid = creditRepository.findById(validCredit.id!!).get()
      assertThat(updatedValid.resolution).isEqualTo(CreditResolution.CREDITED)
    }

    @Test
    @DisplayName("CRD-119 - empty list returns 400")
    fun `CRD-119 should return 400 for empty list`() {
      webTestClient.post()
        .uri("/credits/actions/credit/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("[]")
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    @DisplayName("CRD-110 - unauthenticated request returns 401")
    fun `should return 401 for unauthenticated request`() {
      webTestClient.post()
        .uri("/credits/actions/credit/")
        .header("Content-Type", "application/json")
        .bodyValue("""[{"id": 1, "credited": true}]""")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    @DisplayName("CRD-110 - items with credited=false are skipped and credit state unchanged")
    fun `items with credited=false are not processed`() {
      val credit = createAndSaveCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)

      webTestClient.post()
        .uri("/credits/actions/credit/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""[{"id": ${credit.id}, "credited": false}]""")
        .exchange()
        .expectStatus()
        .isNoContent

      val unchanged = creditRepository.findById(credit.id!!).get()
      assertThat(unchanged.resolution).isEqualTo(CreditResolution.PENDING)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Sender/Payment (CRD-060 to CRD-075)")
  inner class SenderPaymentFilters {

    @Test
    @DisplayName("CRD-060 - Filter sender_name matches transaction.sender_name (case-insensitive substring)")
    fun `should filter by sender_name on transaction`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      createAndSaveTransaction(credit1, senderName = "Alice Johnson")
      createAndSaveTransaction(credit2, senderName = "Bob Smith")

      webTestClient.get()
        .uri("/credits/?sender_name=alice")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-060 - Filter sender_name matches payment.cardholder_name (case-insensitive substring)")
    fun `should filter by sender_name on payment cardholder_name`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      createAndSavePayment(credit1, cardholderName = "Carol Williams")
      createAndSavePayment(credit2, cardholderName = "Dave Brown")

      webTestClient.get()
        .uri("/credits/?sender_name=carol")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-061 - Filter sender_sort_code exact match on transaction")
    fun `should filter by sender_sort_code`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      createAndSaveTransaction(credit1, senderSortCode = "112233")
      createAndSaveTransaction(credit2, senderSortCode = "445566")

      webTestClient.get()
        .uri("/credits/?sender_sort_code=112233")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-062 - Filter sender_account_number exact match on transaction")
    fun `should filter by sender_account_number`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      createAndSaveTransaction(credit1, senderAccountNumber = "12345678")
      createAndSaveTransaction(credit2, senderAccountNumber = "87654321")

      webTestClient.get()
        .uri("/credits/?sender_account_number=12345678")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-063 - Filter sender_roll_number exact match on transaction")
    fun `should filter by sender_roll_number`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      createAndSaveTransaction(credit1, senderRollNumber = "ROLL001")
      createAndSaveTransaction(credit2, senderRollNumber = "ROLL002")

      webTestClient.get()
        .uri("/credits/?sender_roll_number=ROLL001")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-064 - Filter sender_name__isblank=True returns transactions with blank sender_name")
    fun `should filter sender_name__isblank true`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      createAndSaveTransaction(credit1, senderName = null)
      createAndSaveTransaction(credit2, senderName = "Alice Johnson")

      webTestClient.get()
        .uri("/credits/?sender_name__isblank=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-065 - Filter sender_sort_code__isblank=True returns transactions with blank sort code")
    fun `should filter sender_sort_code__isblank true`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      createAndSaveTransaction(credit1, senderSortCode = null)
      createAndSaveTransaction(credit2, senderSortCode = "112233")

      webTestClient.get()
        .uri("/credits/?sender_sort_code__isblank=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-066 - Filter sender_email case-insensitive substring on payment.email")
    fun `should filter by sender_email`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      createAndSavePayment(credit1, email = "alice@example.com")
      createAndSavePayment(credit2, email = "bob@other.com")

      webTestClient.get()
        .uri("/credits/?sender_email=EXAMPLE")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-067 - Filter sender_ip_address exact match on payment")
    fun `should filter by sender_ip_address`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      createAndSavePayment(credit1, ipAddress = "10.0.0.1")
      createAndSavePayment(credit2, ipAddress = "10.0.0.2")

      webTestClient.get()
        .uri("/credits/?sender_ip_address=10.0.0.1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-068 - Filter card_number_first_digits exact match on payment")
    fun `should filter by card_number_first_digits`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      createAndSavePayment(credit1, cardNumberFirstDigits = "411111")
      createAndSavePayment(credit2, cardNumberFirstDigits = "520000")

      webTestClient.get()
        .uri("/credits/?card_number_first_digits=411111")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-069 - Filter card_number_last_digits exact match on payment")
    fun `should filter by card_number_last_digits`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      createAndSavePayment(credit1, cardNumberLastDigits = "1234")
      createAndSavePayment(credit2, cardNumberLastDigits = "5678")

      webTestClient.get()
        .uri("/credits/?card_number_last_digits=1234")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-070 - Filter card_expiry_date exact match on payment")
    fun `should filter by card_expiry_date`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      createAndSavePayment(credit1, cardExpiryDate = "1225")
      createAndSavePayment(credit2, cardExpiryDate = "0626")

      webTestClient.get()
        .uri("/credits/?card_expiry_date=1225")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-071 - Filter sender_postcode normalized matching (ignores spaces and case)")
    fun `should filter by sender_postcode ignoring spaces and case`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val address1 = createAndSaveBillingAddress(postcode = "SW1A 1AA")
      val address2 = createAndSaveBillingAddress(postcode = "EC1A 1BB")
      createAndSavePayment(credit1, billingAddress = address1)
      createAndSavePayment(credit2, billingAddress = address2)

      webTestClient.get()
        .uri("/credits/?sender_postcode=sw1a1aa")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-072 - Filter payment_reference prefix match on first 8 chars of payment UUID")
    fun `should filter by payment_reference prefix`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val uuid1 = UUID.fromString("abcdef12-0000-0000-0000-000000000001")
      val uuid2 = UUID.fromString("99999999-0000-0000-0000-000000000002")
      createAndSavePayment(credit1, uuid = uuid1)
      createAndSavePayment(credit2, uuid = uuid2)

      webTestClient.get()
        .uri("/credits/?payment_reference=abcdef12")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-073 - Filter source=bank_transfer returns credits with transactions")
    fun `should filter by source bank_transfer`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      createAndSaveTransaction(credit1)
      createAndSavePayment(credit2)

      webTestClient.get()
        .uri("/credits/?source=BANK_TRANSFER")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-074 - Filter source=online returns credits with payments")
    fun `should filter by source online`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      createAndSaveTransaction(credit1)
      createAndSavePayment(credit2)

      webTestClient.get()
        .uri("/credits/?source=ONLINE")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-075 - Filter source=unknown returns credits with neither transaction nor payment")
    fun `should filter by source unknown`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.UNKNOWN)
      createAndSaveTransaction(credit1)

      webTestClient.get()
        .uri("/credits/?source=UNKNOWN")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("Credit List Filters - Search & Ordering (CRD-095 to CRD-099)")
  inner class SearchAndOrderingFilters {

    @Test
    @DisplayName("CRD-095 - search={text} matches prisoner_name")
    fun `should search by prisoner_name`() {
      createAndSaveCredit(prisonerName = "John Smith", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerName = "Jane Doe", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?search=John")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_name").isEqualTo("John Smith")
    }

    @Test
    @DisplayName("CRD-095 - search={text} matches prisoner_number")
    fun `should search by prisoner_number`() {
      createAndSaveCredit(prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerNumber = "Z9999XY", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?search=A1234BC")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-095 - search by amount in £nn.nn format")
    fun `should search by amount in pounds format`() {
      createAndSaveCredit(amount = 5000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2500, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri { it.path("/credits/").queryParam("search", "£50.00").build() }
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-095 - search by 8-char payment UUID prefix")
    fun `should search by payment UUID prefix`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.ONLINE)
      val uuid1 = UUID.fromString("abcdef12-0000-0000-0000-000000000001")
      val uuid2 = UUID.fromString("99999999-0000-0000-0000-000000000002")
      createAndSavePayment(credit1, uuid = uuid1)
      createAndSavePayment(credit2, uuid = uuid2)

      webTestClient.get()
        .uri("/credits/?search=abcdef12")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-096 - all search words must match (AND logic)")
    fun `search uses AND logic for multiple words`() {
      createAndSaveCredit(prisonerName = "John Smith", prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerName = "John Doe", prisonerNumber = "Z9999XY", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?search=John+Smith")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
        .jsonPath("$.results[0].prisoner_name").isEqualTo("John Smith")
    }

    @Test
    @DisplayName("CRD-097 - simple_search searches transaction.sender_name")
    fun `simple_search matches transaction sender_name`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED, prison = "LEI", source = CreditSource.BANK_TRANSFER)
      createAndSaveTransaction(credit1, senderName = "Alice Sender")
      createAndSaveTransaction(credit2, senderName = "Bob Other")

      webTestClient.get()
        .uri("/credits/?simple_search=Alice")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-097 - simple_search searches prisoner_number")
    fun `simple_search matches prisoner_number`() {
      createAndSaveCredit(prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerNumber = "Z9999XY", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?simple_search=A1234BC")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.count").isEqualTo(1)
    }

    @Test
    @DisplayName("CRD-098 - ordering by amount ascending")
    fun `should order by amount ascending`() {
      createAndSaveCredit(amount = 3000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2000, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?ordering=amount")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].amount").isEqualTo(1000)
        .jsonPath("$.results[1].amount").isEqualTo(2000)
        .jsonPath("$.results[2].amount").isEqualTo(3000)
    }

    @Test
    @DisplayName("CRD-098 - ordering by amount descending")
    fun `should order by amount descending`() {
      createAndSaveCredit(amount = 3000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 1000, resolution = CreditResolution.CREDITED)
      createAndSaveCredit(amount = 2000, resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?ordering=-amount")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].amount").isEqualTo(3000)
        .jsonPath("$.results[1].amount").isEqualTo(2000)
        .jsonPath("$.results[2].amount").isEqualTo(1000)
    }

    @Test
    @DisplayName("CRD-099 - ordering by prisoner_number ascending")
    fun `should order by prisoner_number ascending`() {
      createAndSaveCredit(prisonerNumber = "C1111CC", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerNumber = "A1111AA", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerNumber = "B1111BB", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?ordering=prisoner_number")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].prisoner_number").isEqualTo("A1111AA")
        .jsonPath("$.results[1].prisoner_number").isEqualTo("B1111BB")
        .jsonPath("$.results[2].prisoner_number").isEqualTo("C1111CC")
    }

    @Test
    @DisplayName("CRD-099 - ordering by prisoner_name descending")
    fun `should order by prisoner_name descending`() {
      createAndSaveCredit(prisonerName = "Charlie", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerName = "Alice", resolution = CreditResolution.CREDITED)
      createAndSaveCredit(prisonerName = "Bob", resolution = CreditResolution.CREDITED)

      webTestClient.get()
        .uri("/credits/?ordering=-prisoner_name")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.results[0].prisoner_name").isEqualTo("Charlie")
        .jsonPath("$.results[1].prisoner_name").isEqualTo("Bob")
        .jsonPath("$.results[2].prisoner_name").isEqualTo("Alice")
    }
  }

  @Nested
  @DisplayName("POST /credits/actions/setmanual/ (CRD-120 to CRD-125)")
  inner class SetManualAction {

    @Test
    @DisplayName("CRD-120 - POST /credits/actions/setmanual/ accepts credit_ids list")
    fun `should accept credit_ids list`() {
      val credit = createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")

      webTestClient.post()
        .uri("/credits/actions/setmanual/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("CRD-121 - Non-pending credits returned as conflict_ids with 200")
    fun `should return 200 with conflict_ids for non-pending credits`() {
      val credited = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.post()
        .uri("/credits/actions/setmanual/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credited.id}]}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.conflict_ids").isArray
        .jsonPath("$.conflict_ids[0]").isEqualTo(credited.id!!.toInt())
    }

    @Test
    @DisplayName("CRD-122 - Sets resolution=MANUAL and owner=user on eligible credits")
    fun `should set resolution to MANUAL and owner`() {
      val credit = createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")

      webTestClient.post()
        .uri("/credits/actions/setmanual/")
        .headers(setAuthorisation(username = "manager1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val updated = creditRepository.findById(credit.id!!).get()
      assertThat(updated.resolution).isEqualTo(CreditResolution.MANUAL)
      assertThat(updated.owner).isEqualTo("manager1")
    }

    @Test
    @DisplayName("CRD-123 - Creates log entry with LogAction.MANUAL")
    fun `should create MANUAL log entry`() {
      val credit = createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")

      webTestClient.post()
        .uri("/credits/actions/setmanual/")
        .headers(setAuthorisation(username = "manager1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val logs = logRepository.findAll().filter { it.credit?.id == credit.id && it.action == LogAction.MANUAL }
      assertThat(logs).hasSize(1)
      assertThat(logs[0].userId).isEqualTo("manager1")
    }

    @Test
    @DisplayName("CRD-125 - Returns 204 on success")
    fun `should return 204 on success`() {
      val credit = createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")

      webTestClient.post()
        .uri("/credits/actions/setmanual/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("CRD-120 - Empty credit_ids list returns 400")
    fun `should return 400 for empty credit_ids`() {
      webTestClient.post()
        .uri("/credits/actions/setmanual/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": []}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("CRD-120 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated setmanual request`() {
      webTestClient.post()
        .uri("/credits/actions/setmanual/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [1]}""")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  @DisplayName("POST /credits/actions/review/ (CRD-130 to CRD-136)")
  inner class ReviewAction {

    @Test
    @DisplayName("CRD-130 - POST /credits/actions/review/ accepts credit_ids list")
    fun `should accept credit_ids list for review`() {
      val credit = createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")

      webTestClient.post()
        .uri("/credits/actions/review/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("CRD-131 - Sets reviewed=true on ALL specified credits regardless of state")
    fun `should set reviewed=true on all credits regardless of state`() {
      val pendingCredit = createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")
      val creditedCredit = createAndSaveCredit(resolution = CreditResolution.CREDITED)
      val refundedCredit = createAndSaveCredit(resolution = CreditResolution.REFUNDED)

      webTestClient.post()
        .uri("/credits/actions/review/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${pendingCredit.id}, ${creditedCredit.id}, ${refundedCredit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(pendingCredit.id!!).get().reviewed).isTrue
      assertThat(creditRepository.findById(creditedCredit.id!!).get().reviewed).isTrue
      assertThat(creditRepository.findById(refundedCredit.id!!).get().reviewed).isTrue
    }

    @Test
    @DisplayName("CRD-132 - Creates log entry with LogAction.REVIEWED for each credit")
    fun `should create REVIEWED log entry for each credit`() {
      val credit1 = createAndSaveCredit(resolution = CreditResolution.PENDING, prison = "LEI")
      val credit2 = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.post()
        .uri("/credits/actions/review/")
        .headers(setAuthorisation(username = "security1"))
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit1.id}, ${credit2.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val logs1 = logRepository.findAll().filter { it.credit?.id == credit1.id && it.action == LogAction.REVIEWED }
      val logs2 = logRepository.findAll().filter { it.credit?.id == credit2.id && it.action == LogAction.REVIEWED }
      assertThat(logs1).hasSize(1)
      assertThat(logs2).hasSize(1)
      assertThat(logs1[0].userId).isEqualTo("security1")
    }

    @Test
    @DisplayName("CRD-136 - Returns 204 on success")
    fun `should return 204 on review success`() {
      val credit = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.post()
        .uri("/credits/actions/review/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    @DisplayName("CRD-130 - Empty credit_ids list returns 400")
    fun `should return 400 for empty credit_ids in review`() {
      webTestClient.post()
        .uri("/credits/actions/review/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": []}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("CRD-130 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated review request`() {
      webTestClient.post()
        .uri("/credits/actions/review/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [1]}""")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  @DisplayName("POST /credits/actions/refund/ (CRD-140 to CRD-144)")
  inner class RefundAction {

    @Test
    @DisplayName("CRD-140 - POST /credits/actions/refund/ marks refund_pending credits for refund")
    fun `should refund refund_pending credits`() {
      val credit = createAndSaveCredit(
        prison = null,
        resolution = CreditResolution.PENDING,
        blocked = false,
        incompleteSenderInfo = false,
      )

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val updated = creditRepository.findById(credit.id!!).get()
      assertThat(updated.resolution).isEqualTo(CreditResolution.REFUNDED)
    }

    @Test
    @DisplayName("CRD-141 - Only refund_pending credits eligible (no prison OR blocked AND pending AND not incompleteSenderInfo)")
    fun `should only refund credits that are refund_pending`() {
      val eligibleCredit = createAndSaveCredit(
        prison = null,
        resolution = CreditResolution.PENDING,
        blocked = false,
        incompleteSenderInfo = false,
      )
      val ineligibleCredit = createAndSaveCredit(
        prison = "LEI",
        resolution = CreditResolution.PENDING,
        blocked = false,
      )

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${eligibleCredit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val updated = creditRepository.findById(eligibleCredit.id!!).get()
      assertThat(updated.resolution).isEqualTo(CreditResolution.REFUNDED)

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${ineligibleCredit.id}]}""")
        .exchange()
        .expectStatus().isEqualTo(409)
    }

    @Test
    @DisplayName("CRD-142 - Sets resolution=REFUNDED on eligible credits")
    fun `should set resolution to REFUNDED`() {
      val credit = createAndSaveCredit(
        prison = null,
        resolution = CreditResolution.PENDING,
        blocked = false,
        incompleteSenderInfo = false,
      )

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credit.id}]}""")
        .exchange()
        .expectStatus().isNoContent

      val updated = creditRepository.findById(credit.id!!).get()
      assertThat(updated.resolution).isEqualTo(CreditResolution.REFUNDED)
    }

    @Test
    @DisplayName("CRD-144 - Returns 409 Conflict on invalid state")
    fun `should return 409 for credits not in refund_pending state`() {
      val credited = createAndSaveCredit(resolution = CreditResolution.CREDITED)

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [${credited.id}]}""")
        .exchange()
        .expectStatus().isEqualTo(409)
    }

    @Test
    @DisplayName("CRD-140 - Empty credit_ids returns 400")
    fun `should return 400 for empty credit_ids in refund`() {
      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": []}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("CRD-140 - Unauthenticated request returns 401")
    fun `should return 401 for unauthenticated refund request`() {
      webTestClient.post()
        .uri("/credits/actions/refund/")
        .header("Content-Type", "application/json")
        .bodyValue("""{"credit_ids": [1]}""")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  @DisplayName("Method Not Allowed")
  inner class MethodNotAllowed {

    @Test
    @DisplayName("PUT /credits/ returns 405")
    fun `should return method not allowed for PUT`() {
      webTestClient.put()
        .uri("/credits/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isEqualTo(405)
    }

    @Test
    @DisplayName("PATCH /credits/ returns 405")
    fun `should return method not allowed for PATCH`() {
      webTestClient.patch()
        .uri("/credits/")
        .headers(setAuthorisation())
        .header("Content-Type", "application/json")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isEqualTo(405)
    }

    @Test
    @DisplayName("DELETE /credits/ returns 405")
    fun `should return method not allowed for DELETE`() {
      webTestClient.delete()
        .uri("/credits/")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isEqualTo(405)
    }
  }
}
