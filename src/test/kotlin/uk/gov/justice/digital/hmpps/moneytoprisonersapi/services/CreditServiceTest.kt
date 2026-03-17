package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.InvalidCreditStateException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CreditServiceTest {

  @Mock
  private lateinit var creditRepository: CreditRepository

  @InjectMocks
  private lateinit var creditService: CreditService

  private fun createCredit(
    id: Long? = null,
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prisonerName: String? = "John Smith",
    prisonerDob: LocalDate? = LocalDate.of(1990, 1, 15),
    prison: String? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    reviewed: Boolean = false,
    reconciled: Boolean = false,
    receivedAt: LocalDateTime? = null,
    owner: String? = null,
  ): Credit = Credit(
    id = id,
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
  )

  @Nested
  @DisplayName("CRD-010: listCompletedCredits excludes initial and failed")
  inner class ListCompletedCredits {

    @Test
    fun `returns credits excluding initial and failed resolutions`() {
      val credits = listOf(
        createCredit(id = 1, resolution = CreditResolution.PENDING),
        createCredit(id = 2, resolution = CreditResolution.CREDITED),
      )
      whenever(
        creditRepository.findByResolutionNotIn(
          listOf(CreditResolution.INITIAL, CreditResolution.FAILED),
        ),
      ).thenReturn(credits)

      val result = creditService.listCompletedCredits()

      assertThat(result).hasSize(2)
    }
  }

  @Nested
  @DisplayName("CRD-011: listAllCredits includes all resolutions")
  inner class ListAllCredits {

    @Test
    fun `returns all credits regardless of resolution`() {
      val credits = listOf(
        createCredit(id = 1, resolution = CreditResolution.INITIAL),
        createCredit(id = 2, resolution = CreditResolution.FAILED),
        createCredit(id = 3, resolution = CreditResolution.PENDING),
      )
      whenever(creditRepository.findAll()).thenReturn(credits)

      val result = creditService.listAllCredits()

      assertThat(result).hasSize(3)
    }
  }

  @Nested
  @DisplayName("CRD-015: Computed credit_pending status")
  inner class CreditPendingStatus {

    @Test
    fun `credit is credit_pending when prison set, pending, not blocked`() {
      val credit = createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `credit is credit_pending when prison set, manual, not blocked`() {
      val credit = createCredit(prison = "LEI", resolution = CreditResolution.MANUAL, blocked = false)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `credit is NOT credit_pending when blocked`() {
      val credit = createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = true)
      assertThat(creditService.computeStatus(credit)).isNotEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `credit is NOT credit_pending when no prison`() {
      val credit = createCredit(prison = null, resolution = CreditResolution.PENDING, blocked = false)
      assertThat(creditService.computeStatus(credit)).isNotEqualTo(CreditStatus.CREDIT_PENDING)
    }
  }

  @Nested
  @DisplayName("CRD-016: Computed credited status")
  inner class CreditedStatus {

    @Test
    fun `credit is credited when resolution is credited`() {
      val credit = createCredit(resolution = CreditResolution.CREDITED)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.CREDITED)
    }
  }

  @Nested
  @DisplayName("CRD-017: Computed refund_pending status")
  inner class RefundPendingStatus {

    @Test
    fun `credit is refund_pending when no prison, pending, not blocked`() {
      val credit = createCredit(prison = null, resolution = CreditResolution.PENDING, blocked = false)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `credit is refund_pending when blocked and pending`() {
      val credit = createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = true)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.REFUND_PENDING)
    }
  }

  @Nested
  @DisplayName("CRD-018: Computed refunded status")
  inner class RefundedStatus {

    @Test
    fun `credit is refunded when resolution is refunded`() {
      val credit = createCredit(resolution = CreditResolution.REFUNDED)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.REFUNDED)
    }
  }

  @Nested
  @DisplayName("CRD-019: Computed failed status")
  inner class FailedStatus {

    @Test
    fun `credit is failed when resolution is failed`() {
      val credit = createCredit(resolution = CreditResolution.FAILED)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.FAILED)
    }
  }

  @Nested
  @DisplayName("Initial resolution status")
  inner class InitialStatus {

    @Test
    fun `credit with initial resolution returns initial status`() {
      val credit = createCredit(resolution = CreditResolution.INITIAL)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.INITIAL)
    }
  }

  @Nested
  @DisplayName("createCredit")
  inner class CreateCredit {

    @Test
    fun `saves and returns a new credit`() {
      val savedCredit = createCredit(id = 1, amount = 5000, prisonerNumber = "A1234BC")
      whenever(creditRepository.save(any<Credit>())).thenReturn(savedCredit)

      val result = creditService.createCredit(
        amount = 5000,
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        prisonerDob = LocalDate.of(1990, 1, 15),
        receivedAt = LocalDateTime.of(2024, 3, 15, 10, 30),
        source = CreditSource.BANK_TRANSFER,
      )

      assertThat(result.id).isEqualTo(1)
      assertThat(result.amount).isEqualTo(5000)
    }

    @Test
    fun `sets source on created credit`() {
      whenever(creditRepository.save(any<Credit>())).thenAnswer { it.arguments[0] }

      creditService.createCredit(
        amount = 5000,
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        prisonerDob = LocalDate.of(1990, 1, 15),
        receivedAt = null,
        source = CreditSource.ONLINE,
      )

      val captor = argumentCaptor<Credit>()
      verify(creditRepository).save(captor.capture())
      assertThat(captor.firstValue.source).isEqualTo(CreditSource.ONLINE)
    }

    @Test
    fun `defaults resolution to initial`() {
      whenever(creditRepository.save(any<Credit>())).thenAnswer { it.arguments[0] }

      creditService.createCredit(
        amount = 5000,
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        prisonerDob = LocalDate.of(1990, 1, 15),
        receivedAt = null,
        source = CreditSource.UNKNOWN,
      )

      val captor = argumentCaptor<Credit>()
      verify(creditRepository).save(captor.capture())
      assertThat(captor.firstValue.resolution).isEqualTo(CreditResolution.INITIAL)
    }
  }

  @Nested
  @DisplayName("CRD-006: Resolution transition via service")
  inner class TransitionResolution {

    @Test
    fun `transitions credit resolution and saves`() {
      val credit = createCredit(id = 1, resolution = CreditResolution.PENDING, prison = "LEI")
      whenever(creditRepository.findById(1L)).thenReturn(java.util.Optional.of(credit))
      whenever(creditRepository.save(any<Credit>())).thenAnswer { it.arguments[0] }

      val result = creditService.transitionResolution(1L, CreditResolution.CREDITED)

      assertThat(result.resolution).isEqualTo(CreditResolution.CREDITED)
      verify(creditRepository).save(credit)
    }

    @Test
    fun `throws exception for invalid transition`() {
      val credit = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findById(1L)).thenReturn(java.util.Optional.of(credit))

      assertThatThrownBy {
        creditService.transitionResolution(1L, CreditResolution.PENDING)
      }.isInstanceOf(InvalidCreditStateException::class.java)
    }

    @Test
    fun `throws exception when credit not found`() {
      whenever(creditRepository.findById(1L)).thenReturn(java.util.Optional.empty())

      assertThatThrownBy {
        creditService.transitionResolution(1L, CreditResolution.CREDITED)
      }.isInstanceOf(CreditNotFoundException::class.java)
        .hasMessage("Credit not found with id: 1")
    }
  }
}
