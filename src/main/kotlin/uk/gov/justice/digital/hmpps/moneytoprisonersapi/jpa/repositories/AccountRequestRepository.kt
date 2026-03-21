package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest

interface AccountRequestRepository : JpaRepository<AccountRequest, Long> {

  @Query("SELECT r FROM AccountRequest r WHERE r.status = 'pending' ORDER BY r.created ASC")
  fun findAllPendingOrderByCreatedAsc(): List<AccountRequest>

  @Query("SELECT r FROM AccountRequest r WHERE r.status = 'pending' ORDER BY r.created DESC")
  fun findAllPendingOrderByCreatedDesc(): List<AccountRequest>
}
