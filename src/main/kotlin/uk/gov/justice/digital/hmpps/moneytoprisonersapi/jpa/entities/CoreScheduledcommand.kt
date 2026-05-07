package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(name = "core_scheduledcommand", schema = "public")
open class CoreScheduledcommand {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  open var name: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "arg_string", nullable = false)
  open var argString: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "cron_entry", nullable = false)
  open var cronEntry: String = ""

  @Column(name = "next_execution")
  open var nextExecution: OffsetDateTime? = null

  @NotNull
  @Column(name = "delete_after_next", nullable = false)
  open var deleteAfterNext: Boolean = false
}
