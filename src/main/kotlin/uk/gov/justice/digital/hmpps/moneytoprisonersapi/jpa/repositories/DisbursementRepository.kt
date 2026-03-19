package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement

@Repository
interface DisbursementRepository : JpaRepository<Disbursement, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT d FROM Disbursement d WHERE d.id IN :ids")
  fun findByIdInWithLock(ids: List<Long>): List<Disbursement>
}
