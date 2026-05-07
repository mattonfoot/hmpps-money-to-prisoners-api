package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

@Entity
@Table(
  name = "mtp_auth_applicationusermapping",
  schema = "public",
  indexes = [
    Index(
      name = "mtp_auth_applicationusermapping_application_id_007249b3",
      columnList = "application_id",
    ),
    Index(
      name = "mtp_auth_applicationusermapping_user_id_fa5f0d51",
      columnList = "user_id",
    ),
  ],
)
open class MtpAuthApplicationusermapping {
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

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  open var application: Oauth2ProviderApplication? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}
