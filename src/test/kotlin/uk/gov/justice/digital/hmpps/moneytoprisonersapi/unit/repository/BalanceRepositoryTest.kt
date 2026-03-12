package uk.gov.justice.digital.hmpps.moneytoprisonersapi.unit.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.model.Balance
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.repository.BalanceRepository
import java.math.BigInteger
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Balance Repository")
class BalanceRepositoryTest {

    @Autowired
    lateinit var balanceRepository: BalanceRepository

    @BeforeEach
    fun setUp() {
        balanceRepository.deleteAll()
    }

    @Test
    @DisplayName("ACC-003: created and modified are auto-populated on save")
    fun `timestamps are auto-populated on save`() {
        val balance = Balance(
            closingBalance = BigInteger.valueOf(5000),
            date = LocalDate.of(2024, 3, 15),
        )

        val saved = balanceRepository.save(balance)

        assertNotNull(saved.created)
        assertNotNull(saved.modified)
    }

    @Test
    @DisplayName("ACC-004: default ordering is newest-first")
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

        val results = balanceRepository.findAllByOrderByDateDesc()

        assertEquals(3, results.size)
        assertEquals(LocalDate.of(2024, 12, 31), results[0].date)
        assertEquals(LocalDate.of(2024, 6, 15), results[1].date)
        assertEquals(LocalDate.of(2024, 1, 1), results[2].date)
    }
}
