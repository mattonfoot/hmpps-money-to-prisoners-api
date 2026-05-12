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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.CustomException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserEvent
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PaymentRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.TransactionRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.UserEventRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
@DisplayName("DeleteOldDataService")
class DeleteOldDataServiceTest {

  @Mock
  private lateinit var transactionRepository: TransactionRepository

  @Mock
  private lateinit var paymentRepository: PaymentRepository

  @Mock
  private lateinit var userEventRepository: UserEventRepository

  @InjectMocks
  private lateinit var service: DeleteOldDataService

  private fun txn(id: Long) = Transaction().apply { this.id = id }
  private fun pmt() = Payment()
  private fun userEvent(id: Long) = UserEvent().apply { this.id = id }

  @Nested
  @DisplayName("resolveCutoffDate (DOD-001)")
  inner class ResolveCutoffDate {

    @Test
    fun `DOD-001 defaults to seven years before today when before=null`() {
      val today = LocalDate.of(2026, 5, 12)
      val expected = today.minusDays(7 * 365L)

      val result = service.resolveCutoffDate(before = null, today = today)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `DOD-002 returns supplied before when older than seven years`() {
      val today = LocalDate.of(2026, 5, 12)
      val older = today.minusYears(8)

      val result = service.resolveCutoffDate(before = older, today = today)

      assertThat(result).isEqualTo(older)
    }

    @Test
    fun `DOD-003 rejects a before date that is newer than seven years ago`() {
      val today = LocalDate.of(2026, 5, 12)
      val tooRecent = today.minusYears(2)

      assertThatThrownBy { service.resolveCutoffDate(before = tooRecent, today = today) }
        .isInstanceOf(CustomException::class.java)
        .hasMessageContaining("older than 7 years")
    }
  }

  @Nested
  @DisplayName("deleteOldTransactions (DOD-010)")
  inner class DeleteOldTransactions {

    @Test
    fun `DOD-010 deletes transactions whose received_at is before the cutoff`() {
      val cutoff = OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val olds = listOf(txn(1), txn(2), txn(3))
      whenever(transactionRepository.findByReceivedAtLessThan(cutoff)).thenReturn(olds)

      val deleted = service.deleteOldTransactions(cutoff)

      verify(transactionRepository).deleteAll(olds)
      assertThat(deleted).isEqualTo(3)
    }

    @Test
    fun `DOD-010 returns zero when nothing matches the cutoff`() {
      val cutoff = OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      whenever(transactionRepository.findByReceivedAtLessThan(cutoff)).thenReturn(emptyList())

      val deleted = service.deleteOldTransactions(cutoff)

      assertThat(deleted).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("deleteOldPayments (DOD-011)")
  inner class DeleteOldPayments {

    @Test
    fun `DOD-011 deletes payments whose modified is before the cutoff`() {
      val cutoff = OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val olds = listOf(pmt(), pmt())
      whenever(paymentRepository.findByModifiedBefore(cutoff)).thenReturn(olds)

      val deleted = service.deleteOldPayments(cutoff)

      verify(paymentRepository).deleteAll(olds)
      assertThat(deleted).isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("deleteOldUserEvents (DOD-012)")
  inner class DeleteOldUserEvents {

    @Test
    fun `DOD-012 deletes user events whose timestamp is before the cutoff`() {
      val cutoff = OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      val olds = listOf(userEvent(1), userEvent(2), userEvent(3), userEvent(4))
      whenever(userEventRepository.findByTimestampBefore(cutoff)).thenReturn(olds)

      val deleted = service.deleteOldUserEvents(cutoff)

      verify(userEventRepository).deleteAll(olds)
      assertThat(deleted).isEqualTo(4)
    }
  }

  @Nested
  @DisplayName("deleteOldData (DOD-020)")
  inner class DeleteOldData {

    @Test
    fun `DOD-020 deletes from all three tables and returns a per-table summary`() {
      val today = LocalDate.of(2026, 5, 12)
      val expectedCutoff = today.minusDays(7 * 365L).atStartOfDay().atOffset(ZoneOffset.UTC)
      whenever(transactionRepository.findByReceivedAtLessThan(expectedCutoff)).thenReturn(listOf(txn(1)))
      whenever(paymentRepository.findByModifiedBefore(expectedCutoff)).thenReturn(listOf(pmt(), pmt()))
      whenever(userEventRepository.findByTimestampBefore(expectedCutoff)).thenReturn(listOf(userEvent(1), userEvent(2), userEvent(3)))

      val summary = service.deleteOldData(before = null, today = today)

      assertThat(summary.transactionsDeleted).isEqualTo(1)
      assertThat(summary.paymentsDeleted).isEqualTo(2)
      assertThat(summary.userEventsDeleted).isEqualTo(3)
      assertThat(summary.cutoff).isEqualTo(expectedCutoff)
    }
  }
}
