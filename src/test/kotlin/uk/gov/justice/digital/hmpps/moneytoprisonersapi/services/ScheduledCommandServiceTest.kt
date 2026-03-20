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
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("ScheduledCommandService")
class ScheduledCommandServiceTest {

  @Mock
  private lateinit var scheduledCommandRepository: ScheduledCommandRepository

  @InjectMocks
  private lateinit var service: ScheduledCommandService

  @Nested
  @DisplayName("findDueCommands")
  inner class FindDueCommands {

    @Test
    fun `returns commands whose nextExecution is now or in the past`() {
      val due = ScheduledCommand(id = 1L, name = "send_notifications", cronEntry = "0 9 * * *", nextExecution = LocalDateTime.now().minusMinutes(5))
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
      val cmd = ScheduledCommand(id = 1L, name = "send_notifications", cronEntry = "0 9 * * *", nextExecution = LocalDateTime.now().minusMinutes(5))
      whenever(scheduledCommandRepository.save(cmd)).thenReturn(cmd)

      service.markExecuted(cmd)

      val captor = argumentCaptor<ScheduledCommand>()
      verify(scheduledCommandRepository).save(captor.capture())
      assertThat(captor.firstValue.nextExecution).isAfter(LocalDateTime.now().minusMinutes(1))
    }

    @Test
    fun `COR-012 deletes command when deleteAfterNext is true`() {
      val cmd = ScheduledCommand(
        id = 1L,
        name = "one_time_task",
        cronEntry = "0 9 * * *",
        nextExecution = LocalDateTime.now().minusMinutes(5),
        deleteAfterNext = true,
      )

      service.markExecuted(cmd)

      verify(scheduledCommandRepository).delete(cmd)
    }
  }
}
