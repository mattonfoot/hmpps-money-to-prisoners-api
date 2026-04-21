package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.ContainersConfig
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Balance
import java.math.BigInteger
import java.time.LocalDate

@DataJpaTest
@Import(ContainersConfig::class)
@DisplayName("Balance Repository")
class BalanceRepositoryTest @Autowired constructor(
  val balanceRepository: BalanceRepository,
  private val entityManager: TestEntityManager,
) {

  private val defaultPageable = PageRequest.of(0, 20)

  @BeforeEach
  fun setup() {
    balanceRepository.deleteAll()
    entityManager.clear()
  }

  @Nested
  @DisplayName("ACC-003: created and modified are auto-populated on save")
  inner class CreatedAndModifiedAreAutoPopulatedOnSave {

    @Test
    fun `timestamps are auto-populated on save`() {
      val balance = Balance(
        closingBalance = BigInteger.valueOf(5000),
        date = LocalDate.of(2024, 3, 15),
      )

      val saved = balanceRepository.save(balance)

      assertNotNull(saved.created)
      assertNotNull(saved.modified)
    }
  }

  @Nested
  @DisplayName("ACC-004: default ordering is newest-first")
  inner class DefaultOrderingIsNewestFirst {

    @Test
    fun `findAll returns balances ordered by date descending`() {
      val older = Balance(
        closingBalance = BigInteger.valueOf(1000),
        date = LocalDate.of(2024, 1, 1),
      )
      val middle = Balance(
        closingBalance = BigInteger.valueOf(2000),
        date = LocalDate.of(2024, 6, 15),
      )
      val newest = Balance(
        closingBalance = BigInteger.valueOf(3000),
        date = LocalDate.of(2024, 12, 31),
      )

      // Save in random order
      balanceRepository.save(middle)
      balanceRepository.save(older)
      balanceRepository.save(newest)

      val results = balanceRepository.findAllByOrderByDateDesc(defaultPageable)

      assertEquals(3, results.content.size)
      assertEquals(LocalDate.of(2024, 12, 31), results.content[0].date)
      assertEquals(LocalDate.of(2024, 6, 15), results.content[1].date)
      assertEquals(LocalDate.of(2024, 1, 1), results.content[2].date)
    }
  }

  @Nested
  @DisplayName("Pagination")
  inner class PaginationTests {

    @Test
    fun `returns correct page when limit and offset provided`() {
      for (i in 1..5) {
        balanceRepository.save(
          Balance(closingBalance = BigInteger.valueOf(i * 100L), date = LocalDate.of(2024, 1, i)),
        )
      }

      val firstPage = balanceRepository.findAllByOrderByDateDesc(PageRequest.of(0, 2))
      val secondPage = balanceRepository.findAllByOrderByDateDesc(PageRequest.of(1, 2))

      assertEquals(2, firstPage.content.size)
      assertEquals(5, firstPage.totalElements)
      assertEquals(LocalDate.of(2024, 1, 5), firstPage.content[0].date)

      assertEquals(2, secondPage.content.size)
      assertEquals(LocalDate.of(2024, 1, 3), secondPage.content[0].date)
    }
  }
}
