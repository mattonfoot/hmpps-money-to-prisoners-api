package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Entity
@Table(name = "performance_usersatisfaction", schema = "public")
open class PerformanceUsersatisfaction {
  @Id
  @Column(name = "date", nullable = false)
  open var id: LocalDate = LocalDate.now()

  @NotNull
  @Column(name = "rated_1", nullable = false)
  open var rated1: Int = 0

  @NotNull
  @Column(name = "rated_2", nullable = false)
  open var rated2: Int = 0

  @NotNull
  @Column(name = "rated_3", nullable = false)
  open var rated3: Int = 0

  @NotNull
  @Column(name = "rated_4", nullable = false)
  open var rated4: Int = 0

  @NotNull
  @Column(name = "rated_5", nullable = false)
  open var rated5: Int = 0
}
