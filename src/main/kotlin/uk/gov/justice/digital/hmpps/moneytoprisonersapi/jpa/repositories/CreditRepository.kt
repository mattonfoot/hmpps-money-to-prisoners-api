package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrison
import java.time.OffsetDateTime

/**
 * Django stores `credit_credit.resolution` as varchar; pass through `String`
 * values rather than the legacy `CreditResolution` enum directly.
 */
@Repository
interface CreditRepository : JpaRepository<Credit, Long> {
  fun findByResolutionNotIn(resolutions: List<String>): List<Credit>
  fun findByResolution(resolution: String): List<Credit>
  fun findByPrison(prison: PrisonPrison): List<Credit>
  fun findByPrisonIsNull(): List<Credit>
  fun findByBlocked(blocked: Boolean): List<Credit>
  fun findByReviewed(reviewed: Boolean): List<Credit>
  fun findByPrisonerNumber(prisonerNumber: String): List<Credit>
  fun existsByPrisonerNumberAndResolution(prisonerNumber: String, resolution: String): Boolean

  fun findByPrisonerNumberAndPrisonIsNull(prisonerNumber: String): List<Credit>

  fun findByReceivedAtGreaterThanEqualAndReceivedAtBefore(
    from: OffsetDateTime,
    to: OffsetDateTime,
  ): List<Credit>

  fun findByResolutionAndReconciledFalseAndReceivedAtGreaterThanEqualAndReceivedAtBefore(
    resolution: String,
    from: OffsetDateTime,
    to: OffsetDateTime,
  ): List<Credit>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM CreditCredit c WHERE c.id IN :ids")
  fun findByIdInWithLock(ids: List<Long>): List<Credit>

  // Convenience: find by owner via auth_user join.
  @Query("SELECT c FROM CreditCredit c WHERE c.owner.username = :username")
  fun findByOwnerUsername(username: String): List<Credit>
}
