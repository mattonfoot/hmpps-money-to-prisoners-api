package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrivateEstateBatch
import java.time.LocalDate

@Repository
interface PrivateEstateBatchRepository : JpaRepository<PrivateEstateBatch, Long> {
  fun findByPrison(prison: PrisonPrison): List<PrivateEstateBatch>
  fun findByPrisonAndDate(prison: PrisonPrison, date: LocalDate): PrivateEstateBatch?

  // Django models the credit↔batch link via `credit_credit.private_estate_batch_id`
  // (FK on the credit), not a M2M junction table. The legacy "clearJoinTable"
  // semantic is satisfied by nulling the FK on every credit referencing any
  // batch — used by tests to detach batches before deleting them.
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query("UPDATE CreditCredit c SET c.privateEstateBatch = NULL WHERE c.privateEstateBatch IS NOT NULL")
  fun clearJoinTable()
}
