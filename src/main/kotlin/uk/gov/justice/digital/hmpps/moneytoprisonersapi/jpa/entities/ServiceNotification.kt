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
@Table(name = "service_notification", schema = "public")
open class ServiceNotification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 30)
  @NotNull
  @Column(name = "target", nullable = false, length = 30)
  open var target: String = ""

  @NotNull
  @Column(name = "level", nullable = false)
  open var level: Short = 0

  @NotNull
  @Column(name = "start", nullable = false)
  open var start: OffsetDateTime = OffsetDateTime.now()

  @Column(name = "\"end\"")
  open var end: OffsetDateTime? = null

  @Size(max = 200)
  @NotNull
  @Column(name = "headline", nullable = false, length = 200)
  open var headline: String = ""

  @NotNull
  @Column(name = "message", nullable = false, length = Integer.MAX_VALUE)
  open var message: String = ""

  @NotNull
  @Column(name = "public", nullable = false)
  open var publicField: Boolean = false
}
