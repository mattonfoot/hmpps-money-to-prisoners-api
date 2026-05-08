package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Batch

@Repository
interface BatchRepository : JpaRepository<Batch, Long> {
  // Django models batches via `credit_processingbatch.user_id` (FK to auth_user).
  // The legacy "owner" semantic mapped to that FK; resolve by user.username.
  fun findByUserUsername(username: String): List<Batch>
}
