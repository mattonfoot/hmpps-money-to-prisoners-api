package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest

interface AccountRequestRepository : JpaRepository<AccountRequest, Long> {

  // Django's mtp_auth_accountrequest has no `status` column — presence of a
  // row implies pending. The two orderings are kept for the legacy API shape.
  @Query("SELECT r FROM MtpAuthAccountrequest r ORDER BY r.created ASC")
  fun findAllPendingOrderByCreatedAsc(): List<AccountRequest>

  @Query("SELECT r FROM MtpAuthAccountrequest r ORDER BY r.created DESC")
  fun findAllPendingOrderByCreatedDesc(): List<AccountRequest>
}
