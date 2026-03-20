package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.scheduling.support.CronExpression
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "scheduled_commands")
class ScheduledCommand(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false, length = 255)
  val name: String,

  @Column(name = "arg_string", nullable = false, length = 500)
  val argString: String = "",

  @Column(name = "cron_entry", nullable = false, length = 255)
  val cronEntry: String,

  @Column(name = "next_execution")
  var nextExecution: LocalDateTime? = null,

  @Column(name = "delete_after_next", nullable = false)
  val deleteAfterNext: Boolean = false,
) {

  /**
   * COR-013: Returns true if now >= nextExecution.
   */
  fun isScheduled(): Boolean {
    val next = nextExecution ?: return false
    return !LocalDateTime.now().isBefore(next)
  }

  /**
   * COR-011: Calculates the next execution time from the cron expression.
   * Prepends "0 " to convert 5-field Unix cron to the 6-field Spring format (adds seconds=0).
   */
  fun updateNextExecution() {
    val springCron = "0 $cronEntry"
    val expression = CronExpression.parse(springCron)
    nextExecution = expression.next(LocalDateTime.now(ZoneId.systemDefault()))
  }

  /**
   * Splits argString by whitespace into individual arguments.
   */
  fun getArgs(): List<String> = if (argString.isBlank()) emptyList() else argString.split("\\s+".toRegex())
}
