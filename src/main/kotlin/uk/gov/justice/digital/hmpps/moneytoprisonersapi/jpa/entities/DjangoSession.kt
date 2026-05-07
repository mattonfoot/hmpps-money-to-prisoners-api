package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "django_session",
  schema = "public",
  indexes = [
    Index(
      name = "django_session_expire_date_a5c62663",
      columnList = "expire_date",
    ),
  ],
)
open class DjangoSession {
  @Id
  @Size(max = 40)
  @Column(name = "session_key", nullable = false, length = 40)
  open var sessionKey: String = ""

  @NotNull
  @Column(name = "session_data", nullable = false, length = Integer.MAX_VALUE)
  open var sessionData: String = ""

  @NotNull
  @Column(name = "expire_date", nullable = false)
  open var expireDate: OffsetDateTime = OffsetDateTime.now()
}
