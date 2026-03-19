package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailNotificationPreferences

@Repository
interface EmailNotificationPreferencesRepository : JpaRepository<EmailNotificationPreferences, Long> {
  fun findByUsername(username: String): EmailNotificationPreferences?
}
