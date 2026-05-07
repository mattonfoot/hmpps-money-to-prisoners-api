package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_monitoredpartialemailaddress",
  schema = "public",
  indexes = [
    Index(
      name = "security_monitoredpartialemailaddress_keyword_755559a0_like",
      columnList = "keyword",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_monitoredpartialemailaddress_keyword_key",
      columnNames = ["keyword"],
    ),
  ],
)
open class SecurityMonitoredpartialemailaddress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "modified", nullable = false)
  open var modified: OffsetDateTime = OffsetDateTime.now()

  @Size(max = 255)
  @NotNull
  @Column(name = "keyword", nullable = false)
  open var keyword: String = ""
}
