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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreditActionItem
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.BillingAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.InvalidCreditStateException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPopulation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CreditServiceTest {

  @Mock
  private lateinit var creditRepository: CreditRepository

  @Mock
  private lateinit var prisonRepository: PrisonRepository

  @Mock
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Mock
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @Mock
  private lateinit var logRepository: LogRepository

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
    incompleteSenderInfo: Boolean = false,
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
    incompleteSenderInfo = incompleteSenderInfo,
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

    @Test
    fun `credit is refund_pending when no prison, pending, and no incomplete sender info`() {
      val credit = createCredit(prison = null, resolution = CreditResolution.PENDING, incompleteSenderInfo = false)
      assertThat(creditService.computeStatus(credit)).isEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `credit is NOT refund_pending when incomplete sender info is true`() {
      val credit = createCredit(prison = null, resolution = CreditResolution.PENDING, incompleteSenderInfo = true)
      assertThat(creditService.computeStatus(credit)).isNotEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `credit is NOT refund_pending when blocked and incomplete sender info`() {
      val credit = createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = true, incompleteSenderInfo = true)
      assertThat(creditService.computeStatus(credit)).isNotEqualTo(CreditStatus.REFUND_PENDING)
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
  @DisplayName("CRD-020: listCredits with filters")
  inner class ListCreditsFiltered {

    @Test
    fun `returns all completed credits when no filters`() {
      val credits = listOf(
        createCredit(id = 1, resolution = CreditResolution.PENDING, prison = "LEI"),
        createCredit(id = 2, resolution = CreditResolution.CREDITED),
      )
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED))).thenReturn(credits)

      val result = creditService.listCredits()

      assertThat(result).hasSize(2)
    }

    @Test
    fun `filters by status credit_pending`() {
      val creditPending = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      val credited = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findAll()).thenReturn(listOf(creditPending, credited))

      val result = creditService.listCredits(status = CreditStatus.CREDIT_PENDING)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `filters by status credited`() {
      val creditPending = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      val credited = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findAll()).thenReturn(listOf(creditPending, credited))

      val result = creditService.listCredits(status = CreditStatus.CREDITED)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun `filters by status refund_pending`() {
      val refundPending = createCredit(id = 1, prison = null, resolution = CreditResolution.PENDING)
      val credited = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findAll()).thenReturn(listOf(refundPending, credited))

      val result = creditService.listCredits(status = CreditStatus.REFUND_PENDING)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `filters by status refunded`() {
      val refunded = createCredit(id = 1, resolution = CreditResolution.REFUNDED)
      val credited = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findAll()).thenReturn(listOf(refunded, credited))

      val result = creditService.listCredits(status = CreditStatus.REFUNDED)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-034 filters by status failed`() {
      val failed = createCredit(id = 1, resolution = CreditResolution.FAILED)
      val credited = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findAll()).thenReturn(listOf(failed, credited))

      val result = creditService.listCredits(status = CreditStatus.FAILED)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-035 invalid status returns empty set when no matching credits`() {
      whenever(creditRepository.findAll())
        .thenReturn(listOf(createCredit(id = 1, resolution = CreditResolution.CREDITED)))

      val result = creditService.listCredits(status = CreditStatus.FAILED)

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRD-040 filters by prison`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      val mdi = createCredit(id = 2, prison = "MDI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei, mdi))

      val result = creditService.listCredits(prisons = listOf("LEI"))

      assertThat(result).hasSize(1)
      assertThat(result[0].prison).isEqualTo("LEI")
    }

    @Test
    fun `CRD-042 filters by prison__isnull=true`() {
      val noPrison = createCredit(id = 1, prison = null, resolution = CreditResolution.PENDING)
      val withPrison = createCredit(id = 2, prison = "LEI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(noPrison, withPrison))

      val result = creditService.listCredits(prisonIsNull = true)

      assertThat(result).hasSize(1)
      assertThat(result[0].prison).isNull()
    }

    @Test
    fun `CRD-046 invalid prison returns empty set`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei))

      val result = creditService.listCredits(prisons = listOf("NONEXISTENT"))

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRD-041 filters by multiple prison IDs`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      val mdi = createCredit(id = 2, prison = "MDI", resolution = CreditResolution.PENDING)
      val bxi = createCredit(id = 3, prison = "BXI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei, mdi, bxi))

      val result = creditService.listCredits(prisons = listOf("LEI", "MDI"))

      assertThat(result).hasSize(2)
      assertThat(result.map { it.prison }).containsExactlyInAnyOrder("LEI", "MDI")
    }

    @Test
    fun `CRD-041 single value in prison list works as exact match`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      val mdi = createCredit(id = 2, prison = "MDI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei, mdi))

      val result = creditService.listCredits(prisons = listOf("LEI"))

      assertThat(result).hasSize(1)
      assertThat(result[0].prison).isEqualTo("LEI")
    }

    @Test
    fun `CRD-043 filters by prison region`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      val mdi = createCredit(id = 2, prison = "MDI", resolution = CreditResolution.PENDING)
      val bxi = createCredit(id = 3, prison = "BXI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei, mdi, bxi))

      val leiPrison = Prison(nomisId = "LEI", region = "Yorkshire and Humber")
      val mdiPrison = Prison(nomisId = "MDI", region = "Yorkshire and Humber")
      whenever(prisonRepository.findByRegionContainingIgnoreCase("Yorkshire"))
        .thenReturn(listOf(leiPrison, mdiPrison))

      val result = creditService.listCredits(prisonRegion = "Yorkshire")

      assertThat(result).hasSize(2)
      assertThat(result.map { it.prison }).containsExactlyInAnyOrder("LEI", "MDI")
    }

    @Test
    fun `CRD-043 prison region filter is case-insensitive`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei))

      val leiPrison = Prison(nomisId = "LEI", region = "Yorkshire and Humber")
      whenever(prisonRepository.findByRegionContainingIgnoreCase("yorkshire"))
        .thenReturn(listOf(leiPrison))

      val result = creditService.listCredits(prisonRegion = "yorkshire")

      assertThat(result).hasSize(1)
    }

    @Test
    fun `CRD-043 prison region no match returns empty set`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei))
      whenever(prisonRepository.findByRegionContainingIgnoreCase("NonExistent"))
        .thenReturn(emptyList())

      val result = creditService.listCredits(prisonRegion = "NonExistent")

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRD-044 filters by prison category`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      val mdi = createCredit(id = 2, prison = "MDI", resolution = CreditResolution.PENDING)
      val bxi = createCredit(id = 3, prison = "BXI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei, mdi, bxi))

      val catB = PrisonCategory(id = 1, name = "Category B")
      val leiPrison = Prison(nomisId = "LEI").also { it.categories = mutableSetOf(catB) }
      whenever(prisonRepository.findByCategoryName("Category B"))
        .thenReturn(listOf(leiPrison))

      val result = creditService.listCredits(prisonCategory = "Category B")

      assertThat(result).hasSize(1)
      assertThat(result[0].prison).isEqualTo("LEI")
    }

    @Test
    fun `CRD-044 prison category no match returns empty set`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei))
      whenever(prisonRepository.findByCategoryName("NonExistent"))
        .thenReturn(emptyList())

      val result = creditService.listCredits(prisonCategory = "NonExistent")

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRD-045 filters by prison population`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      val mdi = createCredit(id = 2, prison = "MDI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei, mdi))

      val adult = PrisonPopulation(id = 1, name = "Adult")
      val leiPrison = Prison(nomisId = "LEI").also { it.populations = mutableSetOf(adult) }
      val mdiPrison = Prison(nomisId = "MDI").also { it.populations = mutableSetOf(adult) }
      whenever(prisonRepository.findByPopulationName("Adult"))
        .thenReturn(listOf(leiPrison, mdiPrison))

      val result = creditService.listCredits(prisonPopulation = "Adult")

      assertThat(result).hasSize(2)
    }

    @Test
    fun `CRD-045 prison population no match returns empty set`() {
      val lei = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(lei))
      whenever(prisonRepository.findByPopulationName("NonExistent"))
        .thenReturn(emptyList())

      val result = creditService.listCredits(prisonPopulation = "NonExistent")

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRD-050 filters by exact amount`() {
      val c1 = createCredit(id = 1, amount = 1000, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 2000, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(amount = 1000L)

      assertThat(result).hasSize(1)
      assertThat(result[0].amount).isEqualTo(1000L)
    }

    @Test
    fun `CRD-051 filters by amount__gte`() {
      val c1 = createCredit(id = 1, amount = 500, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 1000, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, amount = 1500, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(amountGte = 1000L)

      assertThat(result).hasSize(2)
    }

    @Test
    fun `CRD-052 filters by amount__lte`() {
      val c1 = createCredit(id = 1, amount = 500, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 1000, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, amount = 1500, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(amountLte = 1000L)

      assertThat(result).hasSize(2)
    }

    @Test
    fun `CRD-080 filters by prisoner_name case-insensitive substring`() {
      val c1 = createCredit(id = 1, prisonerName = "John Smith", resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, prisonerName = "Jane Doe", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(prisonerName = "john")

      assertThat(result).hasSize(1)
      assertThat(result[0].prisonerName).isEqualTo("John Smith")
    }

    @Test
    fun `CRD-081 filters by prisoner_number exact match`() {
      val c1 = createCredit(id = 1, prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, prisonerNumber = "B5678DE", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(prisonerNumber = "A1234BC")

      assertThat(result).hasSize(1)
    }

    @Test
    fun `CRD-082 filters by user (owner)`() {
      val c1 = createCredit(id = 1, owner = "clerk1", resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, owner = "clerk2", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(user = "clerk1")

      assertThat(result).hasSize(1)
    }

    @Test
    fun `CRD-083 filters by resolution`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.PENDING)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(resolution = CreditResolution.CREDITED)

      assertThat(result).hasSize(1)
      assertThat(result[0].resolution).isEqualTo(CreditResolution.CREDITED)
    }

    @Test
    fun `CRD-084 filters by reviewed=true`() {
      val c1 = createCredit(id = 1, reviewed = true, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, reviewed = false, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(reviewed = true)

      assertThat(result).hasSize(1)
      assertThat(result[0].reviewed).isTrue()
    }

    @Test
    fun `CRD-085 filters by received_at__gte and received_at__lt`() {
      val now = LocalDateTime.of(2024, 3, 15, 10, 0)
      val c1 = createCredit(id = 1, receivedAt = now.minusDays(1), resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, receivedAt = now, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, receivedAt = now.plusDays(1), resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(receivedAtGte = now, receivedAtLt = now.plusDays(1))

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun `CRD-053 filters by amount__endswith`() {
      val c1 = createCredit(id = 1, amount = 1050, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 2050, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, amount = 1099, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(amountEndswith = "50")

      assertThat(result).hasSize(2)
      assertThat(result.map { it.id }).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun `CRD-053 amount__endswith with no matches returns empty`() {
      val c1 = createCredit(id = 1, amount = 1000, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1))

      val result = creditService.listCredits(amountEndswith = "99")

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRD-054 filters by amount__regex`() {
      val c1 = createCredit(id = 1, amount = 1000, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 2000, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, amount = 1500, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(amountRegex = "^1.*")

      assertThat(result).hasSize(2)
      assertThat(result.map { it.id }).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    fun `CRD-054 amount__regex with no matches returns empty`() {
      val c1 = createCredit(id = 1, amount = 1000, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1))

      val result = creditService.listCredits(amountRegex = "^9.*")

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRD-055 filters by exclude_amount__endswith`() {
      val c1 = createCredit(id = 1, amount = 1050, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 2050, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, amount = 1099, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(excludeAmountEndswith = "50")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(3L)
    }

    @Test
    fun `CRD-056 filters by exclude_amount__regex`() {
      val c1 = createCredit(id = 1, amount = 1000, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 2000, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, amount = 1500, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(excludeAmountRegex = "^1.*")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun `CRD-057 multiple amount filters combine with AND`() {
      val c1 = createCredit(id = 1, amount = 500, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 1000, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, amount = 1500, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(amountGte = 800L, amountLte = 1200L)

      assertThat(result).hasSize(1)
      assertThat(result[0].amount).isEqualTo(1000L)
    }

    @Test
    fun `CRD-057 endswith and regex combine with AND`() {
      val c1 = createCredit(id = 1, amount = 1050, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, amount = 2050, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, amount = 1099, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(amountEndswith = "50", amountRegex = "^1.*")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-036 filter valid=true returns credit_pending or credited`() {
      val creditPending = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      val credited = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      val refundPending = createCredit(id = 3, prison = null, resolution = CreditResolution.PENDING)
      whenever(creditRepository.findAll()).thenReturn(listOf(creditPending, credited, refundPending))

      val result = creditService.listCredits(valid = true)

      assertThat(result).hasSize(2)
      assertThat(result.map { it.id }).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun `CRD-037 filter valid=false returns non-valid credits`() {
      val creditPending = createCredit(id = 1, prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      val credited = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      val refundPending = createCredit(id = 3, prison = null, resolution = CreditResolution.PENDING)
      whenever(creditRepository.findAll()).thenReturn(listOf(creditPending, credited, refundPending))

      val result = creditService.listCredits(valid = false)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(3L)
    }

    @Test
    fun `CRD-060 filters by sender_name case-insensitive on transaction sender_name`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, senderName = "John Smith", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.transaction = Transaction(id = 2, senderName = "Jane Doe", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderName = "john")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-060 filters by sender_name case-insensitive on payment cardholder_name`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(cardholderName = "Alice Jones", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(cardholderName = "Bob Brown", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderName = "alice")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-060 sender_name matches across transaction and payment`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, senderName = "John Smith", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(cardholderName = "John Doe", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderName = "john")

      assertThat(result).hasSize(2)
    }

    @Test
    fun `CRD-061 filters by sender_sort_code exact match`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, senderSortCode = "112233", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.transaction = Transaction(id = 2, senderSortCode = "445566", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderSortCode = "112233")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-062 filters by sender_account_number exact match`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, senderAccountNumber = "12345678", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.transaction = Transaction(id = 2, senderAccountNumber = "87654321", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderAccountNumber = "12345678")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-063 filters by sender_roll_number exact match`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, senderRollNumber = "ROLL001", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.transaction = Transaction(id = 2, senderRollNumber = "ROLL002", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderRollNumber = "ROLL001")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-064 filters by sender_name__isblank=true for blank transaction sender_name`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, senderName = "", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.transaction = Transaction(id = 2, senderName = "John", credit = c2)
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      c3.transaction = Transaction(id = 3, senderName = null, credit = c3)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(senderNameIsBlank = true)

      assertThat(result).hasSize(2)
      assertThat(result.map { it.id }).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    fun `CRD-065 filters by sender_sort_code__isblank=true for blank sort code`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, senderSortCode = "", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.transaction = Transaction(id = 2, senderSortCode = "112233", credit = c2)
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      c3.transaction = Transaction(id = 3, senderSortCode = null, credit = c3)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(senderSortCodeIsBlank = true)

      assertThat(result).hasSize(2)
      assertThat(result.map { it.id }).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    fun `CRD-066 filters by sender_email case-insensitive substring on payment email`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(email = "John@Example.com", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(email = "jane@other.com", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderEmail = "example")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-067 filters by sender_ip_address exact match on payment ip_address`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(ipAddress = "192.168.1.1", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(ipAddress = "10.0.0.1", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderIpAddress = "192.168.1.1")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-068 filters by card_number_first_digits exact match`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(cardNumberFirstDigits = "411111", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(cardNumberFirstDigits = "522222", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(cardNumberFirstDigits = "411111")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-069 filters by card_number_last_digits exact match`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(cardNumberLastDigits = "1234", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(cardNumberLastDigits = "5678", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(cardNumberLastDigits = "1234")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-070 filters by card_expiry_date exact match`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(cardExpiryDate = "12/25", credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(cardExpiryDate = "06/26", credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(cardExpiryDate = "12/25")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-071 filters by sender_postcode normalized matching`() {
      val addr1 = BillingAddress(id = 1, postcode = "SW1A 1AA")
      val addr2 = BillingAddress(id = 2, postcode = "EC2A 4NE")
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(billingAddress = addr1, credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(billingAddress = addr2, credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(senderPostcode = "sw1a1aa")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-071 sender_postcode handles spaces and case differences`() {
      val addr1 = BillingAddress(id = 1, postcode = "sw1a1aa")
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(billingAddress = addr1, credit = c1)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1))

      val result = creditService.listCredits(senderPostcode = "SW1A 1AA")

      assertThat(result).hasSize(1)
    }

    @Test
    fun `CRD-072 filters by payment_reference prefix match on payment uuid`() {
      val uuid1 = java.util.UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890")
      val uuid2 = java.util.UUID.fromString("12345678-abcd-ef12-3456-7890abcdef12")
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.payment = Payment(uuid = uuid1, credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(uuid = uuid2, credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(paymentReference = "abcdef12")

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-073 filters by source=bank_transfer returns credits with transactions`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(credit = c2)
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(source = CreditSource.BANK_TRANSFER)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-074 filters by source=online returns credits with payments`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(credit = c2)
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(source = CreditSource.ONLINE)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun `CRD-075 filters by source=unknown returns credits with neither`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.transaction = Transaction(id = 1, credit = c1)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.payment = Payment(credit = c2)
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(source = CreditSource.UNKNOWN)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(3L)
    }

    @Test
    fun `CRD-086 filters by logged_at__gte truncated to UTC date`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.logs.add(Log(id = 1, action = LogAction.CREDITED, credit = c1).also { it.created = LocalDateTime.of(2024, 3, 14, 23, 59) })
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.logs.add(Log(id = 2, action = LogAction.CREDITED, credit = c2).also { it.created = LocalDateTime.of(2024, 3, 15, 10, 0) })
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      c3.logs.add(Log(id = 3, action = LogAction.CREDITED, credit = c3).also { it.created = LocalDateTime.of(2024, 3, 16, 8, 0) })
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(loggedAtGte = LocalDateTime.of(2024, 3, 15, 0, 0))

      assertThat(result).hasSize(2)
      assertThat(result.map { it.id }).containsExactlyInAnyOrder(2L, 3L)
    }

    @Test
    fun `CRD-086 filters by logged_at__lt truncated to UTC date`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.logs.add(Log(id = 1, action = LogAction.CREDITED, credit = c1).also { it.created = LocalDateTime.of(2024, 3, 14, 23, 59) })
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.logs.add(Log(id = 2, action = LogAction.CREDITED, credit = c2).also { it.created = LocalDateTime.of(2024, 3, 15, 10, 0) })
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(loggedAtLt = LocalDateTime.of(2024, 3, 15, 0, 0))

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-086 logged_at filter uses date truncation ignoring time component`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.logs.add(Log(id = 1, action = LogAction.CREDITED, credit = c1).also { it.created = LocalDateTime.of(2024, 3, 15, 0, 0) })
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.logs.add(Log(id = 2, action = LogAction.CREDITED, credit = c2).also { it.created = LocalDateTime.of(2024, 3, 15, 23, 59) })
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(
        loggedAtGte = LocalDateTime.of(2024, 3, 15, 0, 0),
        loggedAtLt = LocalDateTime.of(2024, 3, 16, 0, 0),
      )

      assertThat(result).hasSize(2)
    }

    @Test
    fun `CRD-086 credits with no logs are excluded by logged_at filter`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.logs.add(Log(id = 1, action = LogAction.CREDITED, credit = c2).also { it.created = LocalDateTime.of(2024, 3, 15, 10, 0) })
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(loggedAtGte = LocalDateTime.of(2024, 3, 15, 0, 0))

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun `CRD-087 filters security_check__isnull=true returns credits without security check`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.securityCheck = SecurityCheck(id = 1, credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(securityCheckIsnull = true)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-087 filters security_check__isnull=false returns credits with security check`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.securityCheck = SecurityCheck(id = 1, credit = c2)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(securityCheckIsnull = false)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun `CRD-088 filters security_check__actioned_by__isnull=true returns checks not yet actioned`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.securityCheck = SecurityCheck(id = 1, credit = c1, actionedBy = null)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.securityCheck = SecurityCheck(id = 2, credit = c2, actionedBy = "admin1")
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(securityCheckActionedByIsnull = true)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(1L)
    }

    @Test
    fun `CRD-088 filters security_check__actioned_by__isnull=false returns checks that have been actioned`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      c1.securityCheck = SecurityCheck(id = 1, credit = c1, actionedBy = null)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      c2.securityCheck = SecurityCheck(id = 2, credit = c2, actionedBy = "admin1")
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(securityCheckActionedByIsnull = false)

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun `CRD-089 filters exclude_credit__in excludes specific credit IDs`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(excludeCreditIn = listOf(1L, 3L))

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun `CRD-089 exclude_credit__in with empty list returns all`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(excludeCreditIn = emptyList())

      assertThat(result).hasSize(2)
    }

    @Test
    fun `CRD-090 filters monitored=true returns credits linked to monitored profiles`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))
      whenever(senderProfileRepository.findCreditIdsWithMonitoredSenderProfiles()).thenReturn(setOf(1L))
      whenever(prisonerProfileRepository.findCreditIdsWithMonitoredPrisonerProfiles()).thenReturn(setOf(2L))

      val result = creditService.listCredits(monitored = true)

      assertThat(result).hasSize(2)
      assertThat(result.map { it.id }).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun `CRD-090 filters monitored=true with no monitored profiles returns empty`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1))
      whenever(senderProfileRepository.findCreditIdsWithMonitoredSenderProfiles()).thenReturn(emptySet())
      whenever(prisonerProfileRepository.findCreditIdsWithMonitoredPrisonerProfiles()).thenReturn(emptySet())

      val result = creditService.listCredits(monitored = true)

      assertThat(result).isEmpty()
    }

    @Test
    fun `CRD-091 filters by pk (multiple credit IDs)`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      val c3 = createCredit(id = 3, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2, c3))

      val result = creditService.listCredits(pk = listOf(1L, 3L))

      assertThat(result).hasSize(2)
      assertThat(result.map { it.id }).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    fun `CRD-091 pk with single ID returns one credit`() {
      val c1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val c2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(listOf(CreditResolution.INITIAL, CreditResolution.FAILED)))
        .thenReturn(listOf(c1, c2))

      val result = creditService.listCredits(pk = listOf(2L))

      assertThat(result).hasSize(1)
      assertThat(result[0].id).isEqualTo(2L)
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

  @Nested
  @DisplayName("CRD-095: search full text search")
  inner class SearchFullText {

    @Test
    fun `search matches prisoner_name (case insensitive)`() {
      val credit1 = createCredit(id = 1, prisonerName = "John Smith", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerName = "Jane Doe", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "john")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `search matches prisoner_number (case insensitive)`() {
      val credit1 = createCredit(id = 1, prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerNumber = "B5678DE", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "a1234")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `search matches transaction sender_name (case insensitive)`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val txn1 = Transaction(senderName = "Alice Sender")
      txn1.credit = credit1
      credit1.transaction = txn1

      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "alice")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `search matches payment cardholder_name (case insensitive)`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val payment1 = Payment(cardholderName = "Bob Cardholder")
      payment1.credit = credit1
      credit1.payment = payment1

      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "bob")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `search matches amount in pounds format (exact pence)`() {
      val credit1 = createCredit(id = 1, amount = 500, resolution = CreditResolution.CREDITED) // £5.00
      val credit2 = createCredit(id = 2, amount = 1050, resolution = CreditResolution.CREDITED) // £10.50
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "5.00")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `search matches amount in pounds format with pound sign`() {
      val credit1 = createCredit(id = 1, amount = 500, resolution = CreditResolution.CREDITED) // £5.00
      val credit2 = createCredit(id = 2, amount = 1050, resolution = CreditResolution.CREDITED) // £10.50
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "£5.00")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `search matches amount without pence using startswith`() {
      val credit1 = createCredit(id = 1, amount = 500, resolution = CreditResolution.CREDITED) // £5.00
      val credit2 = createCredit(id = 2, amount = 5050, resolution = CreditResolution.CREDITED) // £50.50
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "5")

      assertThat(result).containsExactly(credit1, credit2)
    }

    @Test
    fun `search matches payment UUID prefix (8 chars)`() {
      val uuid = UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890")
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val payment1 = Payment(uuid = uuid, cardholderName = "Test")
      payment1.credit = credit1
      credit1.payment = payment1

      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "abcdef12")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `search does not match UUID prefix when word is not 8 chars`() {
      val uuid = UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890")
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val payment1 = Payment(uuid = uuid, cardholderName = "Test")
      payment1.credit = credit1
      credit1.payment = payment1
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1))

      val result = creditService.listCredits(search = "abcdef")

      assertThat(result).isEmpty()
    }
  }

  @Nested
  @DisplayName("CRD-096: search AND logic")
  inner class SearchAndLogic {

    @Test
    fun `all search words must match somewhere (AND logic)`() {
      val credit1 = createCredit(id = 1, prisonerName = "John Smith", prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerName = "John Doe", prisonerNumber = "B5678DE", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(search = "john smith")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `words can match across different fields`() {
      val credit1 = createCredit(id = 1, prisonerName = "John Smith", prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1))

      val result = creditService.listCredits(search = "john A1234")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `returns empty when one word does not match`() {
      val credit1 = createCredit(id = 1, prisonerName = "John Smith", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1))

      val result = creditService.listCredits(search = "john nonexistent")

      assertThat(result).isEmpty()
    }
  }

  @Nested
  @DisplayName("CRD-097: simple_search")
  inner class SimpleSearch {

    @Test
    fun `simple_search matches transaction sender_name (case insensitive)`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val txn = Transaction(senderName = "Alice Sender")
      txn.credit = credit1
      credit1.transaction = txn

      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(simpleSearch = "alice")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `simple_search matches payment cardholder_name (case insensitive)`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val payment = Payment(cardholderName = "Bob Cardholder")
      payment.credit = credit1
      credit1.payment = payment

      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(simpleSearch = "bob")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `simple_search matches payment email (case insensitive)`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED)
      val payment = Payment(email = "test@example.com")
      payment.credit = credit1
      credit1.payment = payment

      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(simpleSearch = "test@example")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `simple_search matches prisoner_number (case insensitive)`() {
      val credit1 = createCredit(id = 1, prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerNumber = "B5678DE", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(simpleSearch = "a1234")

      assertThat(result).containsExactly(credit1)
    }

    @Test
    fun `simple_search does not match prisoner_name`() {
      val credit1 = createCredit(id = 1, prisonerName = "John Smith", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1))

      val result = creditService.listCredits(simpleSearch = "john")

      assertThat(result).isEmpty()
    }
  }

  @Nested
  @DisplayName("CRD-098: ordering by created, received_at, amount")
  inner class OrderingByCreatedReceivedAmount {

    @Test
    fun `ordering by created ascending`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED).apply {
        created = LocalDateTime.of(2024, 1, 1, 10, 0)
      }
      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED).apply {
        created = LocalDateTime.of(2024, 1, 2, 10, 0)
      }
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit2, credit1))

      val result = creditService.listCredits(ordering = "created")

      assertThat(result).containsExactly(credit1, credit2)
    }

    @Test
    fun `ordering by created descending`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED).apply {
        created = LocalDateTime.of(2024, 1, 1, 10, 0)
      }
      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED).apply {
        created = LocalDateTime.of(2024, 1, 2, 10, 0)
      }
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(ordering = "-created")

      assertThat(result).containsExactly(credit2, credit1)
    }

    @Test
    fun `ordering by received_at ascending`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED, receivedAt = LocalDateTime.of(2024, 3, 1, 10, 0))
      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED, receivedAt = LocalDateTime.of(2024, 3, 2, 10, 0))
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit2, credit1))

      val result = creditService.listCredits(ordering = "received_at")

      assertThat(result).containsExactly(credit1, credit2)
    }

    @Test
    fun `ordering by received_at descending`() {
      val credit1 = createCredit(id = 1, resolution = CreditResolution.CREDITED, receivedAt = LocalDateTime.of(2024, 3, 1, 10, 0))
      val credit2 = createCredit(id = 2, resolution = CreditResolution.CREDITED, receivedAt = LocalDateTime.of(2024, 3, 2, 10, 0))
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(ordering = "-received_at")

      assertThat(result).containsExactly(credit2, credit1)
    }

    @Test
    fun `ordering by amount ascending`() {
      val credit1 = createCredit(id = 1, amount = 500, resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, amount = 1000, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit2, credit1))

      val result = creditService.listCredits(ordering = "amount")

      assertThat(result).containsExactly(credit1, credit2)
    }

    @Test
    fun `ordering by amount descending`() {
      val credit1 = createCredit(id = 1, amount = 500, resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, amount = 1000, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(ordering = "-amount")

      assertThat(result).containsExactly(credit2, credit1)
    }
  }

  @Nested
  @DisplayName("CRD-099: ordering by prisoner_number, prisoner_name")
  inner class OrderingByPrisonerFields {

    @Test
    fun `ordering by prisoner_number ascending`() {
      val credit1 = createCredit(id = 1, prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerNumber = "B5678DE", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit2, credit1))

      val result = creditService.listCredits(ordering = "prisoner_number")

      assertThat(result).containsExactly(credit1, credit2)
    }

    @Test
    fun `ordering by prisoner_number descending`() {
      val credit1 = createCredit(id = 1, prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerNumber = "B5678DE", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(ordering = "-prisoner_number")

      assertThat(result).containsExactly(credit2, credit1)
    }

    @Test
    fun `ordering by prisoner_name ascending`() {
      val credit1 = createCredit(id = 1, prisonerName = "Alice Adams", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerName = "Bob Brown", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit2, credit1))

      val result = creditService.listCredits(ordering = "prisoner_name")

      assertThat(result).containsExactly(credit1, credit2)
    }

    @Test
    fun `ordering by prisoner_name descending`() {
      val credit1 = createCredit(id = 1, prisonerName = "Alice Adams", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerName = "Bob Brown", resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(ordering = "-prisoner_name")

      assertThat(result).containsExactly(credit2, credit1)
    }

    @Test
    fun `nulls sort last when ordering ascending`() {
      val credit1 = createCredit(id = 1, prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerNumber = null, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit2, credit1))

      val result = creditService.listCredits(ordering = "prisoner_number")

      assertThat(result).containsExactly(credit1, credit2)
    }

    @Test
    fun `nulls sort last when ordering descending`() {
      val credit1 = createCredit(id = 1, prisonerNumber = "A1234BC", resolution = CreditResolution.CREDITED)
      val credit2 = createCredit(id = 2, prisonerNumber = null, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByResolutionNotIn(any())).thenReturn(listOf(credit1, credit2))

      val result = creditService.listCredits(ordering = "-prisoner_number")

      assertThat(result).containsExactly(credit1, credit2)
    }
  }

  @Nested
  @DisplayName("CRD-110 to CRD-119: creditPrisoners action")
  inner class CreditPrisoners {

    private fun creditPendingCredit(id: Long = 1L): Credit = createCredit(
      id = id,
      prison = "LEI",
      resolution = CreditResolution.PENDING,
      blocked = false,
    )

    @Test
    @DisplayName("CRD-111: credits in credit_pending state are processed successfully")
    fun `CRD-111 processes credit_pending credits`() {
      val credit = creditPendingCredit(id = 1L)
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenReturn(credit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.CREDITED))

      val conflictIds = creditService.creditPrisoners(
        listOf(CreditActionItem(id = 1L, credited = true)),
        "clerk1",
      )

      assertThat(conflictIds).isEmpty()
    }

    @Test
    @DisplayName("CRD-111: non-credit_pending credits go to conflict_ids")
    fun `CRD-111 non-credit_pending credits are added to conflict_ids`() {
      val credit = createCredit(id = 2L, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByIdInWithLock(listOf(2L))).thenReturn(listOf(credit))

      val conflictIds = creditService.creditPrisoners(
        listOf(CreditActionItem(id = 2L, credited = true)),
        "clerk1",
      )

      assertThat(conflictIds).containsExactly(2L)
    }

    @Test
    @DisplayName("CRD-113: sets resolution=CREDITED and owner on the credit")
    fun `CRD-113 sets resolution to CREDITED and owner to requesting user`() {
      val credit = creditPendingCredit(id = 1L)
      val captor = argumentCaptor<Credit>()
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(captor.capture())).thenReturn(credit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.CREDITED))

      creditService.creditPrisoners(
        listOf(CreditActionItem(id = 1L, credited = true)),
        "clerk1",
      )

      val saved = captor.firstValue
      assertThat(saved.resolution).isEqualTo(CreditResolution.CREDITED)
      assertThat(saved.owner).isEqualTo("clerk1")
    }

    @Test
    @DisplayName("CRD-113: sets nomis_transaction_id when provided")
    fun `CRD-113 sets nomis_transaction_id when provided`() {
      val credit = creditPendingCredit(id = 1L)
      val captor = argumentCaptor<Credit>()
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(captor.capture())).thenReturn(credit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.CREDITED))

      creditService.creditPrisoners(
        listOf(CreditActionItem(id = 1L, credited = true, nomisTransactionId = "NOMIS-TX-001")),
        "clerk1",
      )

      assertThat(captor.firstValue.nomisTransactionId).isEqualTo("NOMIS-TX-001")
    }

    @Test
    @DisplayName("CRD-114: creates a CREDITED log entry with the user reference")
    fun `CRD-114 creates log entry with LogAction CREDITED and userId`() {
      val credit = creditPendingCredit(id = 1L)
      val logCaptor = argumentCaptor<Log>()
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenReturn(credit)
      whenever(logRepository.save(logCaptor.capture())).thenReturn(Log(action = LogAction.CREDITED))

      creditService.creditPrisoners(
        listOf(CreditActionItem(id = 1L, credited = true)),
        "clerk1",
      )

      val log = logCaptor.firstValue
      assertThat(log.action).isEqualTo(LogAction.CREDITED)
      assertThat(log.userId).isEqualTo("clerk1")
      assertThat(log.credit).isEqualTo(credit)
    }

    @Test
    @DisplayName("CRD-111: items with credited=false are skipped (not processed, not in conflict_ids)")
    fun `CRD-111 items with credited=false are skipped`() {
      val conflictIds = creditService.creditPrisoners(
        listOf(CreditActionItem(id = 1L, credited = false)),
        "clerk1",
      )

      assertThat(conflictIds).isEmpty()
    }

    @Test
    @DisplayName("CRD-112: mix of valid and invalid credits — invalid go to conflict_ids")
    fun `CRD-112 mix of valid and invalid credits returns conflict_ids for invalid`() {
      val validCredit = creditPendingCredit(id = 1L)
      val invalidCredit = createCredit(id = 2L, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByIdInWithLock(listOf(1L, 2L))).thenReturn(listOf(validCredit, invalidCredit))
      whenever(creditRepository.save(any())).thenReturn(validCredit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.CREDITED))

      val conflictIds = creditService.creditPrisoners(
        listOf(
          CreditActionItem(id = 1L, credited = true),
          CreditActionItem(id = 2L, credited = true),
        ),
        "clerk1",
      )

      assertThat(conflictIds).containsExactly(2L)
    }

    @Test
    @DisplayName("CRD-111: blocked credit is not credit_pending and goes to conflict_ids")
    fun `CRD-111 blocked credit is not eligible and goes to conflict_ids`() {
      val blockedCredit = createCredit(id = 3L, prison = "LEI", resolution = CreditResolution.PENDING, blocked = true)
      whenever(creditRepository.findByIdInWithLock(listOf(3L))).thenReturn(listOf(blockedCredit))

      val conflictIds = creditService.creditPrisoners(
        listOf(CreditActionItem(id = 3L, credited = true)),
        "clerk1",
      )

      assertThat(conflictIds).containsExactly(3L)
    }

    @Test
    @DisplayName("CRD-111: credit with no prison is not credit_pending and goes to conflict_ids")
    fun `CRD-111 credit without prison is not eligible and goes to conflict_ids`() {
      val noPrisonCredit = createCredit(id = 4L, prison = null, resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByIdInWithLock(listOf(4L))).thenReturn(listOf(noPrisonCredit))

      val conflictIds = creditService.creditPrisoners(
        listOf(CreditActionItem(id = 4L, credited = true)),
        "clerk1",
      )

      assertThat(conflictIds).containsExactly(4L)
    }
  }

  @Nested
  @DisplayName("CRD-120 to CRD-125: setManual action")
  inner class SetManual {

    private fun pendingCredit(id: Long = 1L): Credit = createCredit(
      id = id,
      resolution = CreditResolution.PENDING,
    )

    @Test
    @DisplayName("CRD-121: pending credits are set to manual successfully")
    fun `CRD-121 processes pending credits`() {
      val credit = pendingCredit(id = 1L)
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenReturn(credit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.MANUAL))

      val conflictIds = creditService.setManual(listOf(1L), "clerk1")

      assertThat(conflictIds).isEmpty()
    }

    @Test
    @DisplayName("CRD-121: non-pending credits go to conflict_ids")
    fun `CRD-121 non-pending credits are added to conflict_ids`() {
      val credit = createCredit(id = 2L, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByIdInWithLock(listOf(2L))).thenReturn(listOf(credit))

      val conflictIds = creditService.setManual(listOf(2L), "clerk1")

      assertThat(conflictIds).containsExactly(2L)
    }

    @Test
    @DisplayName("CRD-122: sets resolution=MANUAL and owner=user")
    fun `CRD-122 sets resolution to MANUAL and owner to requesting user`() {
      val credit = pendingCredit(id = 1L)
      val captor = argumentCaptor<Credit>()
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(captor.capture())).thenReturn(credit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.MANUAL))

      creditService.setManual(listOf(1L), "clerk1")

      val saved = captor.firstValue
      assertThat(saved.resolution).isEqualTo(CreditResolution.MANUAL)
      assertThat(saved.owner).isEqualTo("clerk1")
    }

    @Test
    @DisplayName("CRD-123: creates a MANUAL log entry with the user reference")
    fun `CRD-123 creates log entry with LogAction MANUAL and userId`() {
      val credit = pendingCredit(id = 1L)
      val logCaptor = argumentCaptor<Log>()
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenReturn(credit)
      whenever(logRepository.save(logCaptor.capture())).thenReturn(Log(action = LogAction.MANUAL))

      creditService.setManual(listOf(1L), "clerk1")

      val log = logCaptor.firstValue
      assertThat(log.action).isEqualTo(LogAction.MANUAL)
      assertThat(log.userId).isEqualTo("clerk1")
      assertThat(log.credit).isEqualTo(credit)
    }

    @Test
    @DisplayName("CRD-121: mix of pending and non-pending — non-pending go to conflict_ids")
    fun `CRD-121 mix of pending and non-pending returns conflict_ids for non-pending`() {
      val validCredit = pendingCredit(id = 1L)
      val invalidCredit = createCredit(id = 2L, resolution = CreditResolution.CREDITED)
      whenever(creditRepository.findByIdInWithLock(listOf(1L, 2L))).thenReturn(listOf(validCredit, invalidCredit))
      whenever(creditRepository.save(any())).thenReturn(validCredit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.MANUAL))

      val conflictIds = creditService.setManual(listOf(1L, 2L), "clerk1")

      assertThat(conflictIds).containsExactly(2L)
    }

    @Test
    @DisplayName("CRD-121: initial resolution is not eligible and goes to conflict_ids")
    fun `CRD-121 initial credit is not eligible and goes to conflict_ids`() {
      val credit = createCredit(id = 5L, resolution = CreditResolution.INITIAL)
      whenever(creditRepository.findByIdInWithLock(listOf(5L))).thenReturn(listOf(credit))

      val conflictIds = creditService.setManual(listOf(5L), "clerk1")

      assertThat(conflictIds).containsExactly(5L)
    }

    @Test
    @DisplayName("CRD-121: manual resolution is not eligible and goes to conflict_ids")
    fun `CRD-121 already-manual credit is not eligible and goes to conflict_ids`() {
      val credit = createCredit(id = 6L, resolution = CreditResolution.MANUAL)
      whenever(creditRepository.findByIdInWithLock(listOf(6L))).thenReturn(listOf(credit))

      val conflictIds = creditService.setManual(listOf(6L), "clerk1")

      assertThat(conflictIds).containsExactly(6L)
    }

    @Test
    @DisplayName("CRD-121: empty list returns empty conflict_ids without hitting repository")
    fun `CRD-121 empty list returns empty conflict_ids`() {
      val conflictIds = creditService.setManual(emptyList(), "clerk1")

      assertThat(conflictIds).isEmpty()
    }
  }

  @Nested
  @DisplayName("CRD-130 to CRD-136: review action")
  inner class Review {

    @Test
    @DisplayName("CRD-131: sets reviewed=true on all specified credits regardless of state")
    fun `CRD-131 sets reviewed=true on all credits regardless of resolution`() {
      val credit1 = createCredit(id = 1L, resolution = CreditResolution.PENDING)
      val credit2 = createCredit(id = 2L, resolution = CreditResolution.CREDITED)
      val captor = argumentCaptor<Credit>()
      whenever(creditRepository.findByIdInWithLock(listOf(1L, 2L))).thenReturn(listOf(credit1, credit2))
      whenever(creditRepository.save(captor.capture())).thenReturn(credit1)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.REVIEWED))

      creditService.review(listOf(1L, 2L), "security_staff")

      assertThat(captor.allValues).allSatisfy { assertThat(it.reviewed).isTrue() }
    }

    @Test
    @DisplayName("CRD-131: no state validation — all credits are reviewed even if already reviewed")
    fun `CRD-131 reviews credits even if already reviewed`() {
      val credit = createCredit(id = 1L, reviewed = true, resolution = CreditResolution.PENDING)
      val captor = argumentCaptor<Credit>()
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(captor.capture())).thenReturn(credit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.REVIEWED))

      creditService.review(listOf(1L), "security_staff")

      assertThat(captor.firstValue.reviewed).isTrue()
    }

    @Test
    @DisplayName("CRD-132: creates a REVIEWED log entry for each credit with the user reference")
    fun `CRD-132 creates REVIEWED log entry for each credit`() {
      val credit1 = createCredit(id = 1L, resolution = CreditResolution.PENDING)
      val credit2 = createCredit(id = 2L, resolution = CreditResolution.CREDITED)
      val logCaptor = argumentCaptor<Log>()
      whenever(creditRepository.findByIdInWithLock(listOf(1L, 2L))).thenReturn(listOf(credit1, credit2))
      whenever(creditRepository.save(any())).thenReturn(credit1)
      whenever(logRepository.save(logCaptor.capture())).thenReturn(Log(action = LogAction.REVIEWED))

      creditService.review(listOf(1L, 2L), "security_staff")

      assertThat(logCaptor.allValues).hasSize(2)
      logCaptor.allValues.forEach { log ->
        assertThat(log.action).isEqualTo(LogAction.REVIEWED)
        assertThat(log.userId).isEqualTo("security_staff")
      }
      assertThat(logCaptor.allValues.map { it.credit }).containsExactlyInAnyOrder(credit1, credit2)
    }

    @Test
    @DisplayName("CRD-133: uses findByIdInWithLock for pessimistic locking")
    fun `CRD-133 uses select_for_update via findByIdInWithLock`() {
      val credit = createCredit(id = 1L, resolution = CreditResolution.PENDING)
      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenReturn(credit)
      whenever(logRepository.save(any())).thenReturn(Log(action = LogAction.REVIEWED))

      creditService.review(listOf(1L), "security_staff")

      verify(creditRepository).findByIdInWithLock(listOf(1L))
    }

    @Test
    @DisplayName("CRD-136: empty list does not interact with repository")
    fun `CRD-136 empty list returns without hitting repository`() {
      creditService.review(emptyList(), "security_staff")

      verify(creditRepository, org.mockito.kotlin.never()).findByIdInWithLock(any())
    }
  }
}
