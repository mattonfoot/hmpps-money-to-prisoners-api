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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.RecipientProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SenderProfile
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.RecipientProfileRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.SenderProfileRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("UpdateSecurityProfilesService")
class UpdateSecurityProfilesServiceTest {

  @Mock
  private lateinit var senderProfileRepository: SenderProfileRepository

  @Mock
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @Mock
  private lateinit var recipientProfileRepository: RecipientProfileRepository

  @Mock
  private lateinit var disbursementRepository: DisbursementRepository

  @InjectMocks
  private lateinit var service: UpdateSecurityProfilesService

  private fun credit(id: Long, amount: Long, resolution: CreditResolution = CreditResolution.CREDITED, counted: Boolean = false) = Credit().apply {
    this.id = id
    this.amount = amount
    this.resolution = resolution.value
    this.isCountedInSenderProfileTotal = counted
    this.isCountedInPrisonerProfileTotal = counted
  }

  private fun disbursement(id: Long, amount: Int, resolution: DisbursementResolution = DisbursementResolution.SENT) = Disbursement().apply {
    this.id = id
    this.amount = amount
    this.resolution = resolution.value
  }

  @Nested
  @DisplayName("recalculateSenderProfileTotals (SEC-080)")
  inner class RecalculateSenderProfile {

    @Test
    fun `SEC-080 sums credited credit amounts into credit_total`() {
      val profile = SenderProfile().apply {
        id = 1L
        credits = mutableListOf(credit(1, 100), credit(2, 200), credit(3, 300))
      }
      whenever(senderProfileRepository.save(any<SenderProfile>())).thenAnswer { it.arguments[0] }

      val result = service.recalculateSenderProfileTotals(profile)

      assertThat(result.creditTotal).isEqualTo(600L)
    }

    @Test
    fun `SEC-080 counts credited credits into credit_count`() {
      val profile = SenderProfile().apply {
        id = 1L
        credits = mutableListOf(credit(1, 100), credit(2, 200))
      }
      whenever(senderProfileRepository.save(any<SenderProfile>())).thenAnswer { it.arguments[0] }

      val result = service.recalculateSenderProfileTotals(profile)

      assertThat(result.creditCount).isEqualTo(2L)
    }

    @Test
    fun `SEC-081 ignores credits whose resolution is not CREDITED`() {
      val profile = SenderProfile().apply {
        id = 1L
        credits = mutableListOf(
          credit(1, 100, CreditResolution.CREDITED),
          credit(2, 200, CreditResolution.PENDING),
          credit(3, 300, CreditResolution.REFUNDED),
          credit(4, 50, CreditResolution.FAILED),
        )
      }
      whenever(senderProfileRepository.save(any<SenderProfile>())).thenAnswer { it.arguments[0] }

      val result = service.recalculateSenderProfileTotals(profile)

      assertThat(result.creditCount).isEqualTo(1L)
      assertThat(result.creditTotal).isEqualTo(100L)
    }

    @Test
    fun `SEC-082 marks credited credits as is_counted_in_sender_profile_total`() {
      val c1 = credit(1, 100, CreditResolution.CREDITED, counted = false)
      val c2 = credit(2, 200, CreditResolution.CREDITED, counted = false)
      val pending = credit(3, 300, CreditResolution.PENDING, counted = false)
      val profile = SenderProfile().apply {
        id = 1L
        credits = mutableListOf(c1, c2, pending)
      }
      whenever(senderProfileRepository.save(any<SenderProfile>())).thenAnswer { it.arguments[0] }

      service.recalculateSenderProfileTotals(profile)

      assertThat(c1.isCountedInSenderProfileTotal).isTrue()
      assertThat(c2.isCountedInSenderProfileTotal).isTrue()
      assertThat(pending.isCountedInSenderProfileTotal).isFalse()
    }

    @Test
    fun `zero credits leaves totals at zero`() {
      val profile = SenderProfile().apply {
        id = 1L
        credits = mutableListOf()
      }
      whenever(senderProfileRepository.save(any<SenderProfile>())).thenAnswer { it.arguments[0] }

      val result = service.recalculateSenderProfileTotals(profile)

      assertThat(result.creditCount).isEqualTo(0L)
      assertThat(result.creditTotal).isEqualTo(0L)
    }

    @Test
    fun `persists the updated profile via the repository`() {
      val profile = SenderProfile().apply {
        id = 1L
        credits = mutableListOf(credit(1, 100))
      }
      whenever(senderProfileRepository.save(any<SenderProfile>())).thenAnswer { it.arguments[0] }

      service.recalculateSenderProfileTotals(profile)

      verify(senderProfileRepository).save(profile)
    }
  }

  @Nested
  @DisplayName("recalculatePrisonerProfileTotals (SEC-085)")
  inner class RecalculatePrisonerProfile {

    @Test
    fun `SEC-085 sums credited credit amounts into credit_total and credit_count`() {
      val profile = PrisonerProfile().apply {
        id = 1L
        credits = mutableListOf(credit(1, 100), credit(2, 200))
      }
      whenever(disbursementRepository.findByPrisonerProfile(profile)).thenReturn(emptyList())
      whenever(prisonerProfileRepository.save(any<PrisonerProfile>())).thenAnswer { it.arguments[0] }

      val result = service.recalculatePrisonerProfileTotals(profile)

      assertThat(result.creditCount).isEqualTo(2L)
      assertThat(result.creditTotal).isEqualTo(300L)
    }

    @Test
    fun `SEC-086 sums sent disbursement amounts into disbursement_total and count`() {
      val profile = PrisonerProfile().apply {
        id = 1L
        credits = mutableListOf()
      }
      val sent = disbursement(10, 500, DisbursementResolution.SENT)
      val pending = disbursement(11, 999, DisbursementResolution.PENDING)
      whenever(disbursementRepository.findByPrisonerProfile(profile)).thenReturn(listOf(sent, pending))
      whenever(prisonerProfileRepository.save(any<PrisonerProfile>())).thenAnswer { it.arguments[0] }

      val result = service.recalculatePrisonerProfileTotals(profile)

      assertThat(result.disbursementCount).isEqualTo(1L)
      assertThat(result.disbursementTotal).isEqualTo(500L)
    }

    @Test
    fun `SEC-087 marks credited credits as is_counted_in_prisoner_profile_total`() {
      val c1 = credit(1, 100, CreditResolution.CREDITED, counted = false)
      val pending = credit(2, 999, CreditResolution.PENDING, counted = false)
      val profile = PrisonerProfile().apply {
        id = 1L
        credits = mutableListOf(c1, pending)
      }
      whenever(disbursementRepository.findByPrisonerProfile(profile)).thenReturn(emptyList())
      whenever(prisonerProfileRepository.save(any<PrisonerProfile>())).thenAnswer { it.arguments[0] }

      service.recalculatePrisonerProfileTotals(profile)

      assertThat(c1.isCountedInPrisonerProfileTotal).isTrue()
      assertThat(pending.isCountedInPrisonerProfileTotal).isFalse()
    }
  }

  @Nested
  @DisplayName("recalculateRecipientProfileTotals (SEC-090)")
  inner class RecalculateRecipientProfile {

    @Test
    fun `SEC-090 sums sent disbursement amounts into disbursement_total and count`() {
      val sent1 = disbursement(1, 100, DisbursementResolution.SENT)
      val sent2 = disbursement(2, 250, DisbursementResolution.SENT)
      val pending = disbursement(3, 999, DisbursementResolution.PENDING)
      val profile = RecipientProfile().apply {
        id = 1L
        disbursements = mutableListOf(sent1, sent2, pending)
      }
      whenever(recipientProfileRepository.save(any<RecipientProfile>())).thenAnswer { it.arguments[0] }

      val result = service.recalculateRecipientProfileTotals(profile)

      assertThat(result.disbursementCount).isEqualTo(2L)
      assertThat(result.disbursementTotal).isEqualTo(350L)
    }

    @Test
    fun `zero disbursements leaves totals at zero`() {
      val profile = RecipientProfile().apply {
        id = 1L
        disbursements = mutableListOf()
      }
      whenever(recipientProfileRepository.save(any<RecipientProfile>())).thenAnswer { it.arguments[0] }

      val result = service.recalculateRecipientProfileTotals(profile)

      assertThat(result.disbursementCount).isEqualTo(0L)
      assertThat(result.disbursementTotal).isEqualTo(0L)
    }
  }

  @Nested
  @DisplayName("recalculateAllProfileTotals (SEC-095)")
  inner class RecalculateAll {

    @Test
    fun `SEC-095 recalculates every sender, prisoner and recipient profile`() {
      val sender = SenderProfile().apply {
        id = 1L
        credits = mutableListOf(credit(1, 100))
      }
      val prisoner = PrisonerProfile().apply {
        id = 2L
        credits = mutableListOf(credit(2, 200))
      }
      val recipient = RecipientProfile().apply {
        id = 3L
        disbursements = mutableListOf(disbursement(3, 300, DisbursementResolution.SENT))
      }
      whenever(senderProfileRepository.findAll()).thenReturn(listOf(sender))
      whenever(prisonerProfileRepository.findAll()).thenReturn(listOf(prisoner))
      whenever(recipientProfileRepository.findAll()).thenReturn(listOf(recipient))
      whenever(disbursementRepository.findByPrisonerProfile(prisoner)).thenReturn(emptyList())
      whenever(senderProfileRepository.save(any<SenderProfile>())).thenAnswer { it.arguments[0] }
      whenever(prisonerProfileRepository.save(any<PrisonerProfile>())).thenAnswer { it.arguments[0] }
      whenever(recipientProfileRepository.save(any<RecipientProfile>())).thenAnswer { it.arguments[0] }

      service.recalculateAllProfileTotals()

      verify(senderProfileRepository).save(sender)
      verify(prisonerProfileRepository).save(prisoner)
      verify(recipientProfileRepository).save(recipient)
      assertThat(sender.creditTotal).isEqualTo(100L)
      assertThat(prisoner.creditTotal).isEqualTo(200L)
      assertThat(recipient.disbursementTotal).isEqualTo(300L)
    }
  }
}
