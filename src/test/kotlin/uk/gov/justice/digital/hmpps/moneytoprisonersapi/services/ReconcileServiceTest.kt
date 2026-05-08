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
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrivateEstateBatchRepository
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("ReconcileService")
class ReconcileServiceTest {

  @Mock
  private lateinit var creditRepository: CreditRepository

  @Mock
  private lateinit var logRepository: LogRepository

  @Mock
  private lateinit var privateEstateBatchRepository: PrivateEstateBatchRepository

  @Mock
  private lateinit var userRepository: AuthUserRepository

  @InjectMocks
  private lateinit var reconcileService: ReconcileService

  private fun createCredit(
    id: Long = 1L,
    amount: Long = 1000L,
    prison: Prison? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
  ): Credit = Credit().apply {
    this.id = id
    this.amount = amount
    prisonerNumber = "A1234BC"
    prisonerName = "John Smith"
    this.prison = prison
    this.resolution = resolution.value
  }

  private fun createPublicPrison(nomisId: String = "LEI"): Prison = Prison().apply {
    this.nomisId = nomisId
    name = "Leeds"
    region = "Yorkshire"
    privateEstate = false
  }

  private fun createPrivatePrison(nomisId: String = "PRV"): Prison = Prison().apply {
    this.nomisId = nomisId
    name = "Private Prison"
    region = "South"
    privateEstate = true
  }

  @Nested
  @DisplayName("CRD-190: reconcile() sets reconciled=true on credits")
  inner class ReconcileSetFlag {

    @Test
    @DisplayName("CRD-190 - sets reconciled=true on each credit")
    fun `should set reconciled to true on each credit`() {
      val publicPrison = createPublicPrison("LEI")
      val credit1 = createCredit(id = 1L, prison = publicPrison)
      val credit2 = createCredit(id = 2L, prison = publicPrison)

      whenever(creditRepository.findByIdInWithLock(listOf(1L, 2L))).thenReturn(listOf(credit1, credit2))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(userRepository.findByUsername("clerk1")).thenReturn(MtpUser().apply { username = "clerk1" })

      reconcileService.reconcile(listOf(1L, 2L), "clerk1")

      assertThat(credit1.reconciled).isTrue()
      assertThat(credit2.reconciled).isTrue()
    }

    @Test
    @DisplayName("CRD-190 - empty list does nothing")
    fun `should do nothing for empty list`() {
      reconcileService.reconcile(emptyList(), "clerk1")

      org.mockito.kotlin.verifyNoInteractions(creditRepository, logRepository, privateEstateBatchRepository)
    }
  }

  @Nested
  @DisplayName("CRD-191: reconcile() creates RECONCILED log for each credit")
  inner class ReconcileLog {

    @Test
    @DisplayName("CRD-191 - creates a RECONCILED log entry for each credit")
    fun `should create RECONCILED log for each credit`() {
      val publicPrison = createPublicPrison("LEI")
      val credit = createCredit(id = 1L, prison = publicPrison)
      val reconciler = MtpUser().apply { username = "clerk1" }

      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(userRepository.findByUsername("clerk1")).thenReturn(reconciler)

      val logCaptor = argumentCaptor<Log>()
      whenever(logRepository.save(logCaptor.capture())).thenAnswer { it.arguments[0] }

      reconcileService.reconcile(listOf(1L), "clerk1")

      val savedLog = logCaptor.firstValue
      assertThat(savedLog.action).isEqualTo(LogAction.RECONCILED.value)
      assertThat(savedLog.credit).isEqualTo(credit)
      assertThat(savedLog.user).isEqualTo(reconciler)
    }
  }

  @Nested
  @DisplayName("CRD-192 to CRD-193: reconcile() creates PrivateEstateBatch for private prison credits")
  inner class ReconcilePrivateEstateBatch {

    @Test
    @DisplayName("CRD-192 - creates PrivateEstateBatch for private prison credit")
    fun `should create PrivateEstateBatch for credit in private prison`() {
      val today = LocalDate.now()
      val privatePrison = createPrivatePrison("PRV")
      val credit = createCredit(id = 1L, prison = privatePrison, amount = 2500)

      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(userRepository.findByUsername("clerk1")).thenReturn(MtpUser().apply { username = "clerk1" })
      whenever(privateEstateBatchRepository.findByPrisonAndDate(eq(privatePrison), eq(today))).thenReturn(null)

      val batchCaptor = argumentCaptor<PrivateEstateBatch>()
      whenever(privateEstateBatchRepository.save(batchCaptor.capture())).thenAnswer { it.arguments[0] }

      reconcileService.reconcile(listOf(1L), "clerk1")

      val savedBatch = batchCaptor.firstValue
      assertThat(savedBatch.prison?.nomisId).isEqualTo("PRV")
      assertThat(savedBatch.date).isEqualTo(today)
      assertThat(credit.privateEstateBatch).isEqualTo(savedBatch)
    }

    @Test
    @DisplayName("CRD-192 - public prison credit does not create PrivateEstateBatch")
    fun `should not create PrivateEstateBatch for credit in public prison`() {
      val publicPrison = createPublicPrison("LEI")
      val credit = createCredit(id = 1L, prison = publicPrison, amount = 1000)

      whenever(creditRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(userRepository.findByUsername("clerk1")).thenReturn(MtpUser().apply { username = "clerk1" })

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
      whenever(userRepository.findByUsername("clerk1")).thenReturn(MtpUser().apply { username = "clerk1" })

      reconcileService.reconcile(listOf(1L), "clerk1")

      org.mockito.kotlin.verifyNoInteractions(privateEstateBatchRepository)
    }
  }
}
