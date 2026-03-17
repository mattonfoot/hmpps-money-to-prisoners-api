package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import java.util.UUID

@Repository
interface PaymentRepository : JpaRepository<Payment, UUID> {
  fun findByCreditId(creditId: Long): Payment?
}
