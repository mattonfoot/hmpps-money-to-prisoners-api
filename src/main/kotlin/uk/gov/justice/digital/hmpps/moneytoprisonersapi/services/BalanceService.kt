package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.DuplicateBalanceDateException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Balance
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BalanceRepository
import java.math.BigInteger
import java.time.LocalDate

@Service
class BalanceService(
  private val balanceRepository: BalanceRepository,
) {
  fun listBalances(dateLt: LocalDate?, dateGte: LocalDate?, limit: Int = 20, offset: Int = 0): Page<Balance> {
    val effectiveLimit = if (limit > 0) limit else 20
    val pageNumber = offset / effectiveLimit
    val pageable = PageRequest.of(pageNumber, effectiveLimit)

    return when {
      dateGte != null && dateLt != null ->
        balanceRepository.findByDateGreaterThanEqualAndDateBeforeOrderByDateDesc(dateGte, dateLt, pageable)

      dateGte != null ->
        balanceRepository.findByDateGreaterThanEqualOrderByDateDesc(dateGte, pageable)

      dateLt != null ->
        balanceRepository.findByDateBeforeOrderByDateDesc(dateLt, pageable)

      else ->
        balanceRepository.findAllByOrderByDateDesc(pageable)
    }
  }

  fun createBalance(closingBalance: BigInteger, date: LocalDate): Balance {
    if (balanceRepository.existsByDate(date)) {
      throw DuplicateBalanceDateException(date)
    }
    return balanceRepository.save(Balance(closingBalance = closingBalance, date = date))
  }
}
