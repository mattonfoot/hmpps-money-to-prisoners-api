package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("ScheduledCommand entity")
class ScheduledCommandEntityTest {

  @Nested
  @DisplayName("COR-010: stores command and cron")
  inner class StorageTests {

    @Test
    fun `stores command name`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *")
      assertThat(cmd.name).isEqualTo("send_notifications")
    }

    @Test
    fun `stores cron expression`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *")
      assertThat(cmd.cronEntry).isEqualTo("0 9 * * *")
    }

    @Test
    fun `stores optional arg string`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *", argString = "--verbosity=2")
      assertThat(cmd.argString).isEqualTo("--verbosity=2")
    }

    @Test
    fun `arg string defaults to empty`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *")
      assertThat(cmd.argString).isEmpty()
    }
  }

  @Nested
  @DisplayName("COR-011: next_execution calculated from cron")
  inner class NextExecutionTests {

    @Test
    fun `updateNextExecution sets nextExecution from cron expression`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *")
      cmd.updateNextExecution()
      assertThat(cmd.nextExecution).isNotNull()
    }

    @Test
    fun `nextExecution is in the future after update`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *")
      cmd.updateNextExecution()
      assertThat(cmd.nextExecution).isAfter(LocalDateTime.now().minusMinutes(1))
    }
  }

  @Nested
  @DisplayName("COR-012: delete_after_next flag")
  inner class DeleteAfterNextTests {

    @Test
    fun `deleteAfterNext defaults to false`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *")
      assertThat(cmd.deleteAfterNext).isFalse()
    }

    @Test
    fun `deleteAfterNext can be set to true`() {
      val cmd = ScheduledCommand(name = "one_time_task", cronEntry = "0 9 * * *", deleteAfterNext = true)
      assertThat(cmd.deleteAfterNext).isTrue()
    }
  }

  @Nested
  @DisplayName("COR-013: isScheduled checks now >= nextExecution")
  inner class IsScheduledTests {

    @Test
    fun `isScheduled returns false when nextExecution is in the future`() {
      val cmd = ScheduledCommand(
        name = "send_notifications",
        cronEntry = "0 9 * * *",
        nextExecution = LocalDateTime.now().plusHours(1),
      )
      assertThat(cmd.isScheduled()).isFalse()
    }

    @Test
    fun `isScheduled returns true when nextExecution is in the past`() {
      val cmd = ScheduledCommand(
        name = "send_notifications",
        cronEntry = "0 9 * * *",
        nextExecution = LocalDateTime.now().minusMinutes(1),
      )
      assertThat(cmd.isScheduled()).isTrue()
    }

    @Test
    fun `isScheduled returns false when nextExecution is null`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *")
      assertThat(cmd.isScheduled()).isFalse()
    }
  }

  @Nested
  @DisplayName("COR-014: run updates nextExecution")
  inner class RunTests {

    @Test
    fun `getArgs splits argString by spaces`() {
      val cmd = ScheduledCommand(
        name = "send_notifications",
        cronEntry = "0 9 * * *",
        argString = "--type=email --verbosity=2",
      )
      assertThat(cmd.getArgs()).containsExactly("--type=email", "--verbosity=2")
    }

    @Test
    fun `getArgs returns empty list when argString is blank`() {
      val cmd = ScheduledCommand(name = "send_notifications", cronEntry = "0 9 * * *")
      assertThat(cmd.getArgs()).isEmpty()
    }
  }
}
