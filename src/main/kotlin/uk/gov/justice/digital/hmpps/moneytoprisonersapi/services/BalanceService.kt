package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Balance
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.BalanceRepository
import java.time.LocalDate

@Service
class BalanceService(
  private val balanceRepository: BalanceRepository,
) {
  fun listBalances(dateLt: LocalDate?, dateGte: LocalDate?): List<Balance> = when {
    dateGte != null && dateLt != null ->
      balanceRepository.findByDateGreaterThanEqualAndDateBeforeOrderByDateDesc(dateGte, dateLt)
    dateGte != null ->
      balanceRepository.findByDateGreaterThanEqualOrderByDateDesc(dateGte)
    dateLt != null ->
      balanceRepository.findByDateBeforeOrderByDateDesc(dateLt)
    else ->
      balanceRepository.findAllByOrderByDateDesc()
  }
}
