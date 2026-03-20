package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.ScheduledCommand

interface ScheduledCommandRepository : JpaRepository<ScheduledCommand, Long> {

  @Query("SELECT c FROM ScheduledCommand c WHERE c.nextExecution IS NOT NULL AND c.nextExecution <= CURRENT_TIMESTAMP")
  fun findAllDueForExecution(): List<ScheduledCommand>
}
