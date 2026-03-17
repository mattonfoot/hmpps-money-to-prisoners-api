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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.BillingAddress
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.InvalidCreditStateException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPopulation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CreditServiceTest {

  @Mock
  private lateinit var creditRepository: CreditRepository

  @Mock
  private lateinit var prisonRepository: PrisonRepository

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
