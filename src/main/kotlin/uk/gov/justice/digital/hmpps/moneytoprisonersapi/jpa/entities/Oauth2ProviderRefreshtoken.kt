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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
  name = "oauth2_provider_refreshtoken",
  schema = "public",
  indexes = [
    Index(
      name = "oauth2_provider_refreshtoken_application_id_2d1c311b",
      columnList = "application_id",
    ),
    Index(
      name = "oauth2_provider_refreshtoken_user_id_da837fce",
      columnList = "user_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "oauth2_provider_refreshtoken_token_revoked_af8a5134_uniq",
      columnNames = [
        "token",
        "revoked",
      ],
    ),
    UniqueConstraint(
      name = "oauth2_provider_refreshtoken_access_token_id_key",
      columnNames = ["access_token_id"],
    ),
  ],
)
open class Oauth2ProviderRefreshtoken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long = 0L

  @Size(max = 255)
  @NotNull
  @Column(name = "token", nullable = false)
  open var token: String = ""

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "access_token_id")
  open var accessToken: Oauth2ProviderAccesstoken? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  open var application: Oauth2ProviderApplication? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "updated", nullable = false)
  open var updated: OffsetDateTime = OffsetDateTime.now()

  @Column(name = "revoked")
  open var revoked: OffsetDateTime? = null

  @Column(name = "token_family")
  open var tokenFamily: UUID? = null
}
