package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.ScheduledCommand
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.ScheduledCommandRepository
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("ScheduledCommandService")
class ScheduledCommandServiceTest {

  @Mock
  private lateinit var scheduledCommandRepository: ScheduledCommandRepository

  @InjectMocks
  private lateinit var service: ScheduledCommandService

  private fun makeCommand(
    id: Long = 1L,
    name: String = "send_notifications",
    cronEntry: String = "0 9 * * *",
    nextExecution: OffsetDateTime? = OffsetDateTime.now().minusMinutes(5),
    deleteAfterNext: Boolean = false,
  ) = ScheduledCommand().apply {
    this.id = id
    this.name = name
    this.cronEntry = cronEntry
    this.nextExecution = nextExecution
    this.deleteAfterNext = deleteAfterNext
  }

  @Nested
  @DisplayName("findDueCommands")
  inner class FindDueCommands {

    @Test
    fun `returns commands whose nextExecution is now or in the past`() {
      val due = makeCommand()
      whenever(scheduledCommandRepository.findAllDueForExecution()).thenReturn(listOf(due))

      val result = service.findDueCommands()

      assertThat(result).hasSize(1)
      assertThat(result[0].name).isEqualTo("send_notifications")
    }
  }

  @Nested
  @DisplayName("COR-014: markExecuted updates nextExecution")
  inner class MarkExecuted {

    @Test
    fun `COR-014 updates nextExecution after execution`() {
      val cmd = makeCommand()
      whenever(scheduledCommandRepository.save(cmd)).thenReturn(cmd)

      service.markExecuted(cmd)

      val captor = argumentCaptor<ScheduledCommand>()
      verify(scheduledCommandRepository).save(captor.capture())
      assertThat(captor.firstValue.nextExecution).isAfter(OffsetDateTime.now().minusMinutes(1))
    }

    @Test
    fun `COR-012 deletes command when deleteAfterNext is true`() {
      val cmd = makeCommand(name = "one_time_task", deleteAfterNext = true)

      service.markExecuted(cmd)

      verify(scheduledCommandRepository).delete(cmd)
    }
  }
}
