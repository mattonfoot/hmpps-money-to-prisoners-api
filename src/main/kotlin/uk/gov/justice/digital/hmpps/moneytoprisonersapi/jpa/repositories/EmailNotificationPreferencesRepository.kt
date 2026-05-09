package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailNotificationPreferences

@Repository
interface EmailNotificationPreferencesRepository : JpaRepository<EmailNotificationPreferences, Long> {
  fun findByUserUsername(username: String): EmailNotificationPreferences?

  // Convenience alias for legacy tests; resolves through the user FK.
  fun findByUsername(username: String): EmailNotificationPreferences? = findByUserUsername(username)
}
