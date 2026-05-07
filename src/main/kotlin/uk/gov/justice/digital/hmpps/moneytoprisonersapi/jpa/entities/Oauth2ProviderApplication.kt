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
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "oauth2_provider_application",
  schema = "public",
  indexes = [
    Index(
      name = "oauth2_provider_application_client_id_03f0cc84_like",
      columnList = "client_id",
    ),
    Index(
      name = "oauth2_provider_application_client_secret_53133678",
      columnList = "client_secret",
    ),
    Index(
      name = "oauth2_provider_application_client_secret_53133678_like",
      columnList = "client_secret",
    ),
    Index(
      name = "oauth2_provider_application_user_id_79829054",
      columnList = "user_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "oauth2_provider_application_client_id_key",
      columnNames = ["client_id"],
    ),
  ],
)
open class Oauth2ProviderApplication {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long = 0L

  @Size(max = 100)
  @NotNull
  @Column(name = "client_id", nullable = false, length = 100)
  open var clientId: String = ""

  @NotNull
  @Column(name = "redirect_uris", nullable = false, length = Integer.MAX_VALUE)
  open var redirectUris: String = ""

  @Size(max = 32)
  @NotNull
  @Column(name = "client_type", nullable = false, length = 32)
  open var clientType: String = ""

  @Size(max = 32)
  @NotNull
  @Column(name = "authorization_grant_type", nullable = false, length = 32)
  open var authorizationGrantType: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "client_secret", nullable = false)
  open var clientSecret: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  open var name: String = ""

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  open var user: AuthUser? = null

  @NotNull
  @Column(name = "skip_authorization", nullable = false)
  open var skipAuthorization: Boolean = false

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "updated", nullable = false)
  open var updated: OffsetDateTime = OffsetDateTime.now()

  @Size(max = 5)
  @NotNull
  @Column(name = "algorithm", nullable = false, length = 5)
  open var algorithm: String = ""

  @NotNull
  @Column(name = "post_logout_redirect_uris", nullable = false, length = Integer.MAX_VALUE)
  open var postLogoutRedirectUris: String = ""

  @NotNull
  @Column(name = "hash_client_secret", nullable = false)
  open var hashClientSecret: Boolean = false

  @NotNull
  @Column(name = "allowed_origins", nullable = false, length = Integer.MAX_VALUE)
  open var allowedOrigins: String = ""
}
