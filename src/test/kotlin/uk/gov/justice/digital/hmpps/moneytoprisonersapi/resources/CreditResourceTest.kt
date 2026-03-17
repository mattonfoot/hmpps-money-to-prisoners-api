package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus
import java.time.LocalDate

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
    fun `returns paginated response with computed status`() {
      val credits = listOf(
        createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING),
        createCredit(id = 2, resolution = CreditResolution.CREDITED),
      )
      whenever(creditService.listCompletedCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.count).isEqualTo(2)
      assertThat(response.results).hasSize(2)
      assertThat(response.results[0].status).isEqualTo(CreditStatus.CREDIT_PENDING)
      assertThat(response.results[1].status).isEqualTo(CreditStatus.CREDITED)
    }

    @Test
    fun `returns empty list when no credits exist`() {
      whenever(creditService.listCompletedCredits()).thenReturn(emptyList())

      val response = creditResource.listCredits()

      assertThat(response.count).isEqualTo(0)
      assertThat(response.results).isEmpty()
    }

    @Test
    fun `CRD-015 credit_pending status included in response`() {
      val credits = listOf(createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false))
      whenever(creditService.listCompletedCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.results[0].status).isEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `CRD-017 refund_pending status included in response`() {
      val credits = listOf(createCredit(prison = null, resolution = CreditResolution.PENDING))
      whenever(creditService.listCompletedCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.results[0].status).isEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `CRD-017 incomplete sender info prevents refund_pending`() {
      val credits = listOf(createCredit(prison = null, resolution = CreditResolution.PENDING, incompleteSenderInfo = true))
      whenever(creditService.listCompletedCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.results[0].status).isNotEqualTo(CreditStatus.REFUND_PENDING)
      assertThat(response.results[0].status).isEqualTo(CreditStatus.INITIAL)
    }

    @Test
    fun `CRD-018 refunded status included in response`() {
      val credits = listOf(createCredit(resolution = CreditResolution.REFUNDED))
      whenever(creditService.listCompletedCredits()).thenReturn(credits)

      val response = creditResource.listCredits()

      assertThat(response.results[0].status).isEqualTo(CreditStatus.REFUNDED)
    }
  }
}
