package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.ScheduledCommand
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.ScheduledCommandRepository

@Service
class ScheduledCommandService(
  private val scheduledCommandRepository: ScheduledCommandRepository,
) {

  /**
   * Returns all commands whose next execution time has arrived.
   */
  @Transactional(readOnly = true)
  fun findDueCommands(): List<ScheduledCommand> = scheduledCommandRepository.findAllDueForExecution()

  /**
   * COR-014: Updates the command's next execution time after a run.
   * COR-012: Deletes the command if deleteAfterNext is set.
   */
  @Transactional
  fun markExecuted(command: ScheduledCommand) {
    if (command.deleteAfterNext) {
      scheduledCommandRepository.delete(command)
    } else {
      command.updateNextExecution()
      scheduledCommandRepository.save(command)
    }
  }
}
