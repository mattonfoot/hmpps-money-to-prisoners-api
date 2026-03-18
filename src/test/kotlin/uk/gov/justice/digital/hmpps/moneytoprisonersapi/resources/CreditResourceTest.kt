package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreditActionItem
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("CreditResource")
class CreditResourceTest {

  @Mock
  private lateinit var creditService: CreditService

  @InjectMocks
  private lateinit var creditResource: CreditResource

  private fun createCredit(
    id: Long? = 1L,
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prison: String? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    incompleteSenderInfo: Boolean = false,
  ): Credit {
    val credit = Credit(
      id = id,
      amount = amount,
      prisonerNumber = prisonerNumber,
      prisonerName = "John Smith",
      prisonerDob = LocalDate.of(1990, 1, 15),
      prison = prison,
      resolution = resolution,
      blocked = blocked,
      incompleteSenderInfo = incompleteSenderInfo,
    )
    credit.onCreate()
    return credit
  }

  @Nested
  @DisplayName("GET /credits/")
  inner class ListCredits {

    @Test
    fun `CRD-020 returns paginated response with computed status`() {
      val credits = listOf(
        createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING),
        createCredit(id = 2, resolution = CreditResolution.CREDITED),
      )
      whenever(creditService.listCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.count).isEqualTo(2)
      assertThat(response.results).hasSize(2)
      assertThat(response.results[0].status).isEqualTo(CreditStatus.CREDIT_PENDING)
      assertThat(response.results[1].status).isEqualTo(CreditStatus.CREDITED)
    }

    @Test
    fun `CRD-020 returns empty list when no credits exist`() {
      whenever(creditService.listCredits()).thenReturn(emptyList())

      val response = creditResource.listCredits()

      assertThat(response.count).isEqualTo(0)
      assertThat(response.results).isEmpty()
    }

    @Test
    fun `CRD-015 credit_pending status included in response`() {
      val credits = listOf(createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false))
      whenever(creditService.listCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.results[0].status).isEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `CRD-017 refund_pending status included in response`() {
      val credits = listOf(createCredit(prison = null, resolution = CreditResolution.PENDING))
      whenever(creditService.listCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.results[0].status).isEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `CRD-017 incomplete sender info prevents refund_pending`() {
      val credits = listOf(createCredit(prison = null, resolution = CreditResolution.PENDING, incompleteSenderInfo = true))
      whenever(creditService.listCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.results[0].status).isNotEqualTo(CreditStatus.REFUND_PENDING)
      assertThat(response.results[0].status).isEqualTo(CreditStatus.INITIAL)
    }

    @Test
    fun `CRD-018 refunded status included in response`() {
      val credits = listOf(createCredit(resolution = CreditResolution.REFUNDED))
      whenever(creditService.listCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.results[0].status).isEqualTo(CreditStatus.REFUNDED)
    }

    @Test
    fun `passes status filter to service`() {
      whenever(creditService.listCredits(status = CreditStatus.CREDITED)).thenReturn(emptyList())

      creditResource.listCredits(status = CreditStatus.CREDITED)

      verify(creditService).listCredits(status = CreditStatus.CREDITED)
    }

    @Test
    fun `passes prison filter to service`() {
      whenever(creditService.listCredits(prisons = listOf("LEI"))).thenReturn(emptyList())

      creditResource.listCredits(prison = listOf("LEI"))

      verify(creditService).listCredits(prisons = listOf("LEI"))
    }

    @Test
    fun `passes prisoner_number filter to service`() {
      whenever(creditService.listCredits(prisonerNumber = "A1234BC")).thenReturn(emptyList())

      creditResource.listCredits(prisonerNumber = "A1234BC")

      verify(creditService).listCredits(prisonerNumber = "A1234BC")
    }

    @Test
    fun `passes prisoner_name filter to service`() {
      whenever(creditService.listCredits(prisonerName = "Smith")).thenReturn(emptyList())

      creditResource.listCredits(prisonerName = "Smith")

      verify(creditService).listCredits(prisonerName = "Smith")
    }

    @Test
    fun `passes amount filters to service`() {
      whenever(creditService.listCredits(amount = 1000L, amountGte = 500L, amountLte = 2000L)).thenReturn(emptyList())

      creditResource.listCredits(amount = 1000L, amountGte = 500L, amountLte = 2000L)

      verify(creditService).listCredits(amount = 1000L, amountGte = 500L, amountLte = 2000L)
    }

    @Test
    fun `CRD-053 passes amount__endswith filter to service`() {
      whenever(creditService.listCredits(amountEndswith = "50")).thenReturn(emptyList())

      creditResource.listCredits(amountEndswith = "50")

      verify(creditService).listCredits(amountEndswith = "50")
    }

    @Test
    fun `CRD-054 passes amount__regex filter to service`() {
      whenever(creditService.listCredits(amountRegex = "^1.*")).thenReturn(emptyList())

      creditResource.listCredits(amountRegex = "^1.*")

      verify(creditService).listCredits(amountRegex = "^1.*")
    }

    @Test
    fun `CRD-055 passes exclude_amount__endswith filter to service`() {
      whenever(creditService.listCredits(excludeAmountEndswith = "00")).thenReturn(emptyList())

      creditResource.listCredits(excludeAmountEndswith = "00")

      verify(creditService).listCredits(excludeAmountEndswith = "00")
    }

    @Test
    fun `CRD-056 passes exclude_amount__regex filter to service`() {
      whenever(creditService.listCredits(excludeAmountRegex = "^1.*")).thenReturn(emptyList())

      creditResource.listCredits(excludeAmountRegex = "^1.*")

      verify(creditService).listCredits(excludeAmountRegex = "^1.*")
    }

    @Test
    fun `passes resolution filter to service`() {
      whenever(creditService.listCredits(resolution = CreditResolution.CREDITED)).thenReturn(emptyList())

      creditResource.listCredits(resolution = CreditResolution.CREDITED)

      verify(creditService).listCredits(resolution = CreditResolution.CREDITED)
    }

    @Test
    fun `passes reviewed filter to service`() {
      whenever(creditService.listCredits(reviewed = true)).thenReturn(emptyList())

      creditResource.listCredits(reviewed = true)

      verify(creditService).listCredits(reviewed = true)
    }

    @Test
    fun `passes received_at date range to service`() {
      val gte = LocalDateTime.of(2024, 1, 1, 0, 0)
      val lt = LocalDateTime.of(2024, 2, 1, 0, 0)
      whenever(creditService.listCredits(receivedAtGte = gte, receivedAtLt = lt)).thenReturn(emptyList())

      creditResource.listCredits(receivedAtGte = gte, receivedAtLt = lt)

      verify(creditService).listCredits(receivedAtGte = gte, receivedAtLt = lt)
    }

    @Test
    fun `passes user filter to service`() {
      whenever(creditService.listCredits(user = "clerk1")).thenReturn(emptyList())

      creditResource.listCredits(user = "clerk1")

      verify(creditService).listCredits(user = "clerk1")
    }

    @Test
    fun `passes valid filter to service`() {
      whenever(creditService.listCredits(valid = true)).thenReturn(emptyList())

      creditResource.listCredits(valid = true)

      verify(creditService).listCredits(valid = true)
    }

    @Test
    fun `passes prison__isnull filter to service`() {
      whenever(creditService.listCredits(prisonIsNull = true)).thenReturn(emptyList())

      creditResource.listCredits(prisonIsNull = true)

      verify(creditService).listCredits(prisonIsNull = true)
    }

    @Test
    fun `CRD-041 passes multiple prison IDs to service`() {
      val prisons = listOf("LEI", "MDI")
      whenever(creditService.listCredits(prisons = prisons)).thenReturn(emptyList())

      creditResource.listCredits(prison = prisons)

      verify(creditService).listCredits(prisons = prisons)
    }

    @Test
    fun `CRD-043 passes prison_region filter to service`() {
      whenever(creditService.listCredits(prisonRegion = "Yorkshire")).thenReturn(emptyList())

      creditResource.listCredits(prisonRegion = "Yorkshire")

      verify(creditService).listCredits(prisonRegion = "Yorkshire")
    }

    @Test
    fun `CRD-044 passes prison_category filter to service`() {
      whenever(creditService.listCredits(prisonCategory = "Category B")).thenReturn(emptyList())

      creditResource.listCredits(prisonCategory = "Category B")

      verify(creditService).listCredits(prisonCategory = "Category B")
    }

    @Test
    fun `CRD-045 passes prison_population filter to service`() {
      whenever(creditService.listCredits(prisonPopulation = "Adult")).thenReturn(emptyList())

      creditResource.listCredits(prisonPopulation = "Adult")

      verify(creditService).listCredits(prisonPopulation = "Adult")
    }

    @Test
    fun `CRD-060 passes sender_name filter to service`() {
      whenever(creditService.listCredits(senderName = "John")).thenReturn(emptyList())

      creditResource.listCredits(senderName = "John")

      verify(creditService).listCredits(senderName = "John")
    }

    @Test
    fun `CRD-061 passes sender_sort_code filter to service`() {
      whenever(creditService.listCredits(senderSortCode = "112233")).thenReturn(emptyList())

      creditResource.listCredits(senderSortCode = "112233")

      verify(creditService).listCredits(senderSortCode = "112233")
    }

    @Test
    fun `CRD-062 passes sender_account_number filter to service`() {
      whenever(creditService.listCredits(senderAccountNumber = "12345678")).thenReturn(emptyList())

      creditResource.listCredits(senderAccountNumber = "12345678")

      verify(creditService).listCredits(senderAccountNumber = "12345678")
    }

    @Test
    fun `CRD-063 passes sender_roll_number filter to service`() {
      whenever(creditService.listCredits(senderRollNumber = "ROLL001")).thenReturn(emptyList())

      creditResource.listCredits(senderRollNumber = "ROLL001")

      verify(creditService).listCredits(senderRollNumber = "ROLL001")
    }

    @Test
    fun `CRD-064 passes sender_name__isblank filter to service`() {
      whenever(creditService.listCredits(senderNameIsBlank = true)).thenReturn(emptyList())

      creditResource.listCredits(senderNameIsBlank = true)

      verify(creditService).listCredits(senderNameIsBlank = true)
    }

    @Test
    fun `CRD-065 passes sender_sort_code__isblank filter to service`() {
      whenever(creditService.listCredits(senderSortCodeIsBlank = true)).thenReturn(emptyList())

      creditResource.listCredits(senderSortCodeIsBlank = true)

      verify(creditService).listCredits(senderSortCodeIsBlank = true)
    }

    @Test
    fun `CRD-066 passes sender_email filter to service`() {
      whenever(creditService.listCredits(senderEmail = "test@example.com")).thenReturn(emptyList())

      creditResource.listCredits(senderEmail = "test@example.com")

      verify(creditService).listCredits(senderEmail = "test@example.com")
    }

    @Test
    fun `CRD-067 passes sender_ip_address filter to service`() {
      whenever(creditService.listCredits(senderIpAddress = "192.168.1.1")).thenReturn(emptyList())

      creditResource.listCredits(senderIpAddress = "192.168.1.1")

      verify(creditService).listCredits(senderIpAddress = "192.168.1.1")
    }

    @Test
    fun `CRD-068 passes card_number_first_digits filter to service`() {
      whenever(creditService.listCredits(cardNumberFirstDigits = "411111")).thenReturn(emptyList())

      creditResource.listCredits(cardNumberFirstDigits = "411111")

      verify(creditService).listCredits(cardNumberFirstDigits = "411111")
    }

    @Test
    fun `CRD-069 passes card_number_last_digits filter to service`() {
      whenever(creditService.listCredits(cardNumberLastDigits = "1234")).thenReturn(emptyList())

      creditResource.listCredits(cardNumberLastDigits = "1234")

      verify(creditService).listCredits(cardNumberLastDigits = "1234")
    }

    @Test
    fun `CRD-070 passes card_expiry_date filter to service`() {
      whenever(creditService.listCredits(cardExpiryDate = "12/25")).thenReturn(emptyList())

      creditResource.listCredits(cardExpiryDate = "12/25")

      verify(creditService).listCredits(cardExpiryDate = "12/25")
    }

    @Test
    fun `CRD-071 passes sender_postcode filter to service`() {
      whenever(creditService.listCredits(senderPostcode = "SW1A 1AA")).thenReturn(emptyList())

      creditResource.listCredits(senderPostcode = "SW1A 1AA")

      verify(creditService).listCredits(senderPostcode = "SW1A 1AA")
    }

    @Test
    fun `CRD-072 passes payment_reference filter to service`() {
      whenever(creditService.listCredits(paymentReference = "abcdef12")).thenReturn(emptyList())

      creditResource.listCredits(paymentReference = "abcdef12")

      verify(creditService).listCredits(paymentReference = "abcdef12")
    }

    @Test
    fun `CRD-073 passes source=bank_transfer filter to service`() {
      whenever(creditService.listCredits(source = CreditSource.BANK_TRANSFER)).thenReturn(emptyList())

      creditResource.listCredits(source = CreditSource.BANK_TRANSFER)

      verify(creditService).listCredits(source = CreditSource.BANK_TRANSFER)
    }

    @Test
    fun `CRD-074 passes source=online filter to service`() {
      whenever(creditService.listCredits(source = CreditSource.ONLINE)).thenReturn(emptyList())

      creditResource.listCredits(source = CreditSource.ONLINE)

      verify(creditService).listCredits(source = CreditSource.ONLINE)
    }

    @Test
    fun `CRD-075 passes source=unknown filter to service`() {
      whenever(creditService.listCredits(source = CreditSource.UNKNOWN)).thenReturn(emptyList())

      creditResource.listCredits(source = CreditSource.UNKNOWN)

      verify(creditService).listCredits(source = CreditSource.UNKNOWN)
    }

    @Test
    fun `CRD-086 passes logged_at__gte and logged_at__lt filters to service`() {
      val gte = LocalDateTime.of(2024, 3, 15, 0, 0)
      val lt = LocalDateTime.of(2024, 3, 16, 0, 0)
      whenever(creditService.listCredits(loggedAtGte = gte, loggedAtLt = lt)).thenReturn(emptyList())

      creditResource.listCredits(loggedAtGte = gte, loggedAtLt = lt)

      verify(creditService).listCredits(loggedAtGte = gte, loggedAtLt = lt)
    }

    @Test
    fun `CRD-087 passes security_check__isnull filter to service`() {
      whenever(creditService.listCredits(securityCheckIsnull = true)).thenReturn(emptyList())

      creditResource.listCredits(securityCheckIsnull = true)

      verify(creditService).listCredits(securityCheckIsnull = true)
    }

    @Test
    fun `CRD-088 passes security_check__actioned_by__isnull filter to service`() {
      whenever(creditService.listCredits(securityCheckActionedByIsnull = true)).thenReturn(emptyList())

      creditResource.listCredits(securityCheckActionedByIsnull = true)

      verify(creditService).listCredits(securityCheckActionedByIsnull = true)
    }

    @Test
    fun `CRD-089 passes exclude_credit__in filter to service`() {
      whenever(creditService.listCredits(excludeCreditIn = listOf(1L, 2L))).thenReturn(emptyList())

      creditResource.listCredits(excludeCreditIn = listOf(1L, 2L))

      verify(creditService).listCredits(excludeCreditIn = listOf(1L, 2L))
    }

    @Test
    fun `CRD-090 passes monitored filter to service`() {
      whenever(creditService.listCredits(monitored = true)).thenReturn(emptyList())

      creditResource.listCredits(monitored = true)

      verify(creditService).listCredits(monitored = true)
    }

    @Test
    fun `CRD-091 passes pk filter to service`() {
      whenever(creditService.listCredits(pk = listOf(1L, 3L))).thenReturn(emptyList())

      creditResource.listCredits(pk = listOf(1L, 3L))

      verify(creditService).listCredits(pk = listOf(1L, 3L))
    }

    @Test
    fun `CRD-095 passes search parameter to service`() {
      whenever(creditService.listCredits(search = "john smith")).thenReturn(emptyList())

      creditResource.listCredits(search = "john smith")

      verify(creditService).listCredits(search = "john smith")
    }

    @Test
    fun `CRD-097 passes simple_search parameter to service`() {
      whenever(creditService.listCredits(simpleSearch = "alice")).thenReturn(emptyList())

      creditResource.listCredits(simpleSearch = "alice")

      verify(creditService).listCredits(simpleSearch = "alice")
    }

    @Test
    fun `CRD-098 passes ordering parameter to service`() {
      whenever(creditService.listCredits(ordering = "-created")).thenReturn(emptyList())

      creditResource.listCredits(ordering = "-created")

      verify(creditService).listCredits(ordering = "-created")
    }
  }

  @Nested
  @DisplayName("POST /credits/actions/credit/")
  inner class CreditPrisoners {

    private fun mockPrincipal(username: String = "clerk1"): Principal = Principal { username }

    @Test
    @DisplayName("CRD-118: returns 204 No Content when all credits are processed with no conflicts")
    fun `CRD-118 returns 204 when no conflict ids`() {
      val items = listOf(CreditActionItem(id = 1L, credited = true))
      whenever(creditService.creditPrisoners(items, "clerk1")).thenReturn(emptyList())

      val response = creditResource.creditPrisoners(items, mockPrincipal("clerk1"))

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
      assertThat(response.body).isNull()
    }

    @Test
    @DisplayName("CRD-112: returns 200 with conflict_ids when some credits are in wrong state")
    fun `CRD-112 returns 200 with conflict_ids when there are conflicts`() {
      val items = listOf(
        CreditActionItem(id = 1L, credited = true),
        CreditActionItem(id = 2L, credited = true),
      )
      whenever(creditService.creditPrisoners(items, "clerk1")).thenReturn(listOf(2L))

      val response = creditResource.creditPrisoners(items, mockPrincipal("clerk1"))

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isNotNull
    }

    @Test
    @DisplayName("CRD-119: returns 400 for empty list")
    fun `CRD-119 returns 400 for empty list`() {
      val response = creditResource.creditPrisoners(emptyList(), mockPrincipal())

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("CRD-113: passes items and userId to service")
    fun `CRD-113 passes items and user to service`() {
      val items = listOf(CreditActionItem(id = 1L, credited = true, nomisTransactionId = "TX-001"))
      whenever(creditService.creditPrisoners(items, "clerk1")).thenReturn(emptyList())

      creditResource.creditPrisoners(items, mockPrincipal("clerk1"))

      verify(creditService).creditPrisoners(items, "clerk1")
    }
  }
}
