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
@Table(name = "service_downtime", schema = "public")
open class ServiceDowntime {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 50)
  @NotNull
  @Column(name = "service", nullable = false, length = 50)
  open var service: String = ""

  @NotNull
  @Column(name = "start", nullable = false)
  open var start: OffsetDateTime = OffsetDateTime.now()

  @Column(name = "\"end\"")
  open var end: OffsetDateTime? = null

  @Size(max = 255)
  @NotNull
  @Column(name = "message_to_users", nullable = false)
  open var messageToUsers: String = ""
}
