package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Balance
import java.time.LocalDate

@Repository
interface BalanceRepository : JpaRepository<Balance, Long> {
  fun findAllByOrderByDateDesc(pageable: Pageable): Page<Balance>
  fun findByDateBeforeOrderByDateDesc(date: LocalDate, pageable: Pageable): Page<Balance>
  fun findByDateGreaterThanEqualOrderByDateDesc(date: LocalDate, pageable: Pageable): Page<Balance>
  fun findByDateGreaterThanEqualAndDateBeforeOrderByDateDesc(dateGte: LocalDate, dateLt: LocalDate, pageable: Pageable): Page<Balance>
  fun existsByDate(date: LocalDate): Boolean
}
