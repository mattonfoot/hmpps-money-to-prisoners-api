package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("ReconcileService")
class ReconcileServiceTest {

  @Mock
  private lateinit var creditRepository: CreditRepository

  @Mock
  private lateinit var prisonRepository: PrisonRepository

  @Mock
  private lateinit var logRepository: LogRepository

  @Mock
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @InjectMocks
  private lateinit var reconcileService: ReconcileService

  private fun createCredit(
    id: Long = 1L,
    amount: Long = 1000,
    prison: String? = "LEI",
    resolution: CreditResolution = CreditResolution.PENDING,
  ): Credit {
    val credit = Credit(
      id = id,
      amount = amount,
      prisonerNumber = "A1234BC",
      prisonerName = "John Smith",
      prison = prison,
      resolution = resolution,
    )
    credit.source = CreditSource.BANK_TRANSFER
    return credit
  }

  private fun createPublicPrison(nomisId: String = "LEI"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Leeds", region = "Yorkshire")
    prison.privateEstate = false
    return prison
  }

  private fun createPrivatePrison(nomisId: String = "PRV"): Prison {
    val prison = Prison(nomisId = nomisId, name = "Private Prison", region = "South")
    prison.privateEstate = true
    return prison
  }

  @Nested
  @DisplayName("CRD-190: reconcile() sets reconciled=true on credits")
  inner class ReconcileSetFlag {

    @Test
    @DisplayName("CRD-190 - sets reconciled=true on each credit")
    fun `should set reconciled to true on each credit`() {
      val credit1 = createCredit(id = 1L, prison = "LEI")
      val credit2 = createCredit(id = 2L, prison = "LEI")

      whenever(creditRepository.findByIdInWithLock(listOf(1L, 2L))).thenReturn(listOf(credit1, credit2))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(prisonRepository.findById("LEI")).thenReturn(Optional.of(createPublicPrison("LEI")))

      reconcileService.reconcile(listOf(1L, 2L), "clerk1")

      assertThat(credit1.reconciled).isTrue()
      assertThat(credit2.reconciled).isTrue()
    }

    @Test
    @DisplayName("CRD-190 - empty list does nothing")
    fun `should do nothing for empty list`() {
      reconcileService.reconcile(emptyList<Long>(), "clerk1")

      org.mockito.kotlin.verifyNoInteractions(creditRepository, logRepository, privateEstateBatchRepository)
    }
  }

  @Nested
  @DisplayName("CRD-191: reconcile() creates RECONCILED log for each credit")
  inner class ReconcileLog {

    @Test
    @DisplayName("CRD-191 - creates a RECONCILED log entry for each credit")
    fun `should create RECONCILED log for each credit`() {
      val credit = createCredit(id = 1L, prison = "LEI")

      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(prisonRepository.findById("LEI")).thenReturn(Optional.of(createPublicPrison("LEI")))

      val logCaptor = argumentCaptor<Log>()
      whenever(logRepository.save(logCaptor.capture())).thenAnswer { it.arguments[0] }

      reconcileService.reconcile(listOf(1L), "clerk1")

      val savedLog = logCaptor.firstValue
      assertThat(savedLog.action).isEqualTo(LogAction.RECONCILED)
      assertThat(savedLog.credit).isEqualTo(credit)
      assertThat(savedLog.userId).isEqualTo("clerk1")
    }
  }

  @Nested
  @DisplayName("CRD-192 to CRD-193: reconcile() creates PrivateEstateBatch for private prison credits")
  inner class ReconcilePrivateEstateBatch {

    @Test
    @DisplayName("CRD-192 - creates PrivateEstateBatch for private prison credit")
    fun `should create PrivateEstateBatch for credit in private prison`() {
      val today = LocalDate.now()
      val credit = createCredit(id = 1L, prison = "PRV", amount = 2500)
      val privatePrison = createPrivatePrison("PRV")

      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(prisonRepository.findById("PRV")).thenReturn(Optional.of(privatePrison))
      whenever(privateEstateBatchRepository.findById("PRV/$today")).thenReturn(Optional.empty())

      val batchCaptor = argumentCaptor<PrivateEstateBatch>()
      whenever(privateEstateBatchRepository.save(batchCaptor.capture())).thenAnswer { it.arguments[0] }

      reconcileService.reconcile(listOf(1L), "clerk1")

      val savedBatch = batchCaptor.firstValue
      assertThat(savedBatch.prison).isEqualTo("PRV")
      assertThat(savedBatch.date).isEqualTo(today)
      assertThat(savedBatch.credits).contains(credit)
    }

    @Test
    @DisplayName("CRD-193 - batch ref is PRISON/YYYY-MM-DD and total_amount is sum of credit amounts")
    fun `should set correct ref and total_amount on batch`() {
      val today = LocalDate.now()
      val credit1 = createCredit(id = 1L, prison = "PRV", amount = 1000)
      val credit2 = createCredit(id = 2L, prison = "PRV", amount = 2500)
      val privatePrison = createPrivatePrison("PRV")

      whenever(creditRepository.findByIdInWithLock(listOf(1L, 2L))).thenReturn(listOf(credit1, credit2))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(prisonRepository.findById("PRV")).thenReturn(Optional.of(privatePrison))

      val existingBatch = PrivateEstateBatch(ref = "PRV/$today", prison = "PRV", date = today, totalAmount = 0)
      whenever(privateEstateBatchRepository.findById("PRV/$today")).thenReturn(Optional.of(existingBatch))

      val batchCaptor = argumentCaptor<PrivateEstateBatch>()
      whenever(privateEstateBatchRepository.save(batchCaptor.capture())).thenAnswer { it.arguments[0] }

      reconcileService.reconcile(listOf(1L, 2L), "clerk1")

      val savedBatch = batchCaptor.lastValue
      assertThat(savedBatch.ref).isEqualTo("PRV/$today")
      assertThat(savedBatch.totalAmount).isEqualTo(3500L)
    }

    @Test
    @DisplayName("CRD-192 - public prison credit does not create PrivateEstateBatch")
    fun `should not create PrivateEstateBatch for credit in public prison`() {
      val credit = createCredit(id = 1L, prison = "LEI", amount = 1000)
      val publicPrison = createPublicPrison("LEI")

      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(prisonRepository.findById("LEI")).thenReturn(Optional.of(publicPrison))

      reconcileService.reconcile(listOf(1L), "clerk1")

      org.mockito.kotlin.verifyNoInteractions(privateEstateBatchRepository)
    }

    @Test
    @DisplayName("CRD-192 - credit with no prison does not create PrivateEstateBatch")
    fun `should not create PrivateEstateBatch for credit with no prison`() {
      val credit = createCredit(id = 1L, prison = null, amount = 1000)

      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }

      reconcileService.reconcile(listOf(1L), "clerk1")

      org.mockito.kotlin.verifyNoInteractions(privateEstateBatchRepository)
      org.mockito.kotlin.verifyNoInteractions(prisonRepository)
    }
  }
}
