package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PaymentBatch
import java.time.LocalDate

@Repository
interface PaymentBatchRepository : JpaRepository<PaymentBatch, Long> {
  fun findBySettlementDate(date: LocalDate): List<PaymentBatch>

  @Query("SELECT MAX(pb.refCode) FROM PaymentBatch pb")
  fun findMaxRefCode(): Int?
}
