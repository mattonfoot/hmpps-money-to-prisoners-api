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
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Balance
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BalanceRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.DuplicateBalanceDateException
import java.math.BigInteger
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class BalanceServiceTest {

  @Mock
  private lateinit var balanceRepository: BalanceRepository

  @InjectMocks
  private lateinit var balanceService: BalanceService

  private fun createBalance(id: Long, closingBalance: Long, date: LocalDate): Balance = Balance(id = id, closingBalance = BigInteger.valueOf(closingBalance), date = date)

  @Nested
  @DisplayName("listBalances")
  inner class ListBalances {

    @Test
    @DisplayName("returns all balances ordered by date descending when no filters")
    fun `should return all balances when no filters provided`() {
      val balances = listOf(
        createBalance(2, 200, LocalDate.of(2024, 1, 2)),
        createBalance(1, 100, LocalDate.of(2024, 1, 1)),
      )
      whenever(balanceRepository.findAllByOrderByDateDesc()).thenReturn(balances)

      val result = balanceService.listBalances(dateLt = null, dateGte = null)

      assertThat(result).hasSize(2)
      assertThat(result[0].id).isEqualTo(2)
      assertThat(result[1].id).isEqualTo(1)
    }

    @Test
    @DisplayName("filters by date__lt only")
    fun `should filter by dateLt`() {
      val balances = listOf(
        createBalance(1, 100, LocalDate.of(2024, 1, 1)),
      )
      whenever(balanceRepository.findByDateBeforeOrderByDateDesc(LocalDate.of(2024, 1, 15))).thenReturn(balances)

      val result = balanceService.listBalances(dateLt = LocalDate.of(2024, 1, 15), dateGte = null)

      assertThat(result).hasSize(1)
      assertThat(result[0].date).isEqualTo(LocalDate.of(2024, 1, 1))
    }

    @Test
    @DisplayName("filters by date__gte only")
    fun `should filter by dateGte`() {
      val balances = listOf(
        createBalance(2, 200, LocalDate.of(2024, 1, 15)),
      )
      whenever(balanceRepository.findByDateGreaterThanEqualOrderByDateDesc(LocalDate.of(2024, 1, 15))).thenReturn(balances)

      val result = balanceService.listBalances(dateLt = null, dateGte = LocalDate.of(2024, 1, 15))

      assertThat(result).hasSize(1)
      assertThat(result[0].date).isEqualTo(LocalDate.of(2024, 1, 15))
    }

    @Test
    @DisplayName("filters by combined date range")
    fun `should filter by date range`() {
      val balances = listOf(
        createBalance(2, 200, LocalDate.of(2024, 1, 15)),
      )
      whenever(
        balanceRepository.findByDateGreaterThanEqualAndDateBeforeOrderByDateDesc(
          LocalDate.of(2024, 1, 10),
          LocalDate.of(2024, 1, 20),
        ),
      ).thenReturn(balances)

      val result = balanceService.listBalances(
        dateLt = LocalDate.of(2024, 1, 20),
        dateGte = LocalDate.of(2024, 1, 10),
      )

      assertThat(result).hasSize(1)
      assertThat(result[0].date).isEqualTo(LocalDate.of(2024, 1, 15))
    }

    @Test
    @DisplayName("returns empty list when no balances match")
    fun `should return empty list when no results`() {
      whenever(balanceRepository.findAllByOrderByDateDesc()).thenReturn(emptyList())

      val result = balanceService.listBalances(dateLt = null, dateGte = null)

      assertThat(result).isEmpty()
    }
  }

  @Nested
  @DisplayName("createBalance")
  inner class CreateBalance {

    @Test
    @DisplayName("saves and returns a new balance")
    fun `should save and return a new balance`() {
      val date = LocalDate.of(2024, 1, 15)
      val closingBalance = BigInteger.valueOf(12345)
      val savedBalance = Balance(id = 1, closingBalance = closingBalance, date = date)

      whenever(balanceRepository.existsByDate(date)).thenReturn(false)
      whenever(balanceRepository.save(any<Balance>())).thenReturn(savedBalance)

      val result = balanceService.createBalance(closingBalance = closingBalance, date = date)

      assertThat(result.id).isEqualTo(1)
      assertThat(result.closingBalance).isEqualTo(closingBalance)
      assertThat(result.date).isEqualTo(date)
    }

    @Test
    @DisplayName("ACC-024 - supports large values")
    fun `should handle large closing balance values`() {
      val date = LocalDate.of(2024, 1, 15)
      val largeBalance = BigInteger.valueOf(Long.MAX_VALUE)
      val savedBalance = Balance(id = 1, closingBalance = largeBalance, date = date)

      whenever(balanceRepository.existsByDate(date)).thenReturn(false)
      whenever(balanceRepository.save(any<Balance>())).thenReturn(savedBalance)

      val result = balanceService.createBalance(closingBalance = largeBalance, date = date)

      assertThat(result.closingBalance).isEqualTo(largeBalance)
    }

    @Test
    @DisplayName("ACC-025 - zero balance allowed")
    fun `should allow zero closing balance`() {
      val date = LocalDate.of(2024, 1, 15)
      val zeroBalance = BigInteger.ZERO
      val savedBalance = Balance(id = 1, closingBalance = zeroBalance, date = date)

      whenever(balanceRepository.existsByDate(date)).thenReturn(false)
      whenever(balanceRepository.save(any<Balance>())).thenReturn(savedBalance)

      val result = balanceService.createBalance(closingBalance = zeroBalance, date = date)

      assertThat(result.closingBalance).isEqualTo(BigInteger.ZERO)
    }

    @Test
    @DisplayName("ACC-022/ACC-023 - throws exception for duplicate date")
    fun `should throw exception when balance for date already exists`() {
      val date = LocalDate.of(2024, 1, 15)

      whenever(balanceRepository.existsByDate(date)).thenReturn(true)

      assertThatThrownBy {
        balanceService.createBalance(closingBalance = BigInteger.valueOf(100), date = date)
      }
        .isInstanceOf(DuplicateBalanceDateException::class.java)
        .hasMessage("Balance exists for date 2024-01-15")
    }
  }
}
