package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "performance_performancedata", schema = "public")
open class PerformancePerformancedatum {
  @Id
  @Column(name = "week", nullable = false)
  open var id: LocalDate = LocalDate.now()

  @Column(name = "credits_total")
  open var creditsTotal: Int? = null

  @Column(name = "credits_by_mtp")
  open var creditsByMtp: Int? = null

  @Column(name = "digital_takeup")
  open var digitalTakeup: Double? = null

  @Column(name = "completion_rate")
  open var completionRate: Double? = null

  @Column(name = "user_satisfaction")
  open var userSatisfaction: Double? = null

  @Column(name = "rated_1")
  open var rated1: Int? = null

  @Column(name = "rated_2")
  open var rated2: Int? = null

  @Column(name = "rated_3")
  open var rated3: Int? = null

  @Column(name = "rated_4")
  open var rated4: Int? = null

  @Column(name = "rated_5")
  open var rated5: Int? = null
}
