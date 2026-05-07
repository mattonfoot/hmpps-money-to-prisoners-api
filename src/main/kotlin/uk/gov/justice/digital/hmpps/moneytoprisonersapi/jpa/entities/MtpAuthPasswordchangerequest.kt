package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
  name = "mtp_auth_passwordchangerequest",
  schema = "public",
  indexes = [
    Index(
      name = "mtp_auth_passwordchangerequest_user_id_30ba8c9a",
      columnList = "user_id",
    ),
  ],
)
open class MtpAuthPasswordchangerequest {
  @Id
  @Column(name = "code", nullable = false)
  open var id: UUID? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "modified", nullable = false)
  open var modified: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}
