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
  name = "oauth2_provider_grant",
  schema = "public",
  indexes = [
    Index(
      name = "oauth2_provider_grant_code_49ab4ddf_like",
      columnList = "code",
    ),
    Index(
      name = "oauth2_provider_grant_application_id_81923564",
      columnList = "application_id",
    ),
    Index(
      name = "oauth2_provider_grant_user_id_e8f62af8",
      columnList = "user_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "oauth2_provider_grant_code_key",
      columnNames = ["code"],
    ),
  ],
)
open class Oauth2ProviderGrant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long = 0L

  @Size(max = 255)
  @NotNull
  @Column(name = "code", nullable = false)
  open var code: String = ""

  @NotNull
  @Column(name = "expires", nullable = false)
  open var expires: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "redirect_uri", nullable = false, length = Integer.MAX_VALUE)
  open var redirectUri: String = ""

  @NotNull
  @Column(name = "scope", nullable = false, length = Integer.MAX_VALUE)
  open var scope: String = ""

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

  @Size(max = 128)
  @NotNull
  @Column(name = "code_challenge", nullable = false, length = 128)
  open var codeChallenge: String = ""

  @Size(max = 10)
  @NotNull
  @Column(name = "code_challenge_method", nullable = false, length = 10)
  open var codeChallengeMethod: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "nonce", nullable = false)
  open var nonce: String = ""

  @NotNull
  @Column(name = "claims", nullable = false, length = Integer.MAX_VALUE)
  open var claims: String = ""
}
