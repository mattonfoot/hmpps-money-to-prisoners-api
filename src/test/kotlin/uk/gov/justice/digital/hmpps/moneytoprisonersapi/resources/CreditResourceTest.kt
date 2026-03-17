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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus
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
      whenever(creditService.listCredits(prison = "LEI")).thenReturn(emptyList())

      creditResource.listCredits(prison = "LEI")

      verify(creditService).listCredits(prison = "LEI")
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
  }
}
