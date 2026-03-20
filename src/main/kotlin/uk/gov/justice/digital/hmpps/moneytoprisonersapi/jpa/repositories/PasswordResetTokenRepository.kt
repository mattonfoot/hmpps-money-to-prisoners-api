package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PasswordResetToken
import java.util.UUID

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
  fun findByTokenAndUsedFalse(token: UUID): PasswordResetToken?
}
