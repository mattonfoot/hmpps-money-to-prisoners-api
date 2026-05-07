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

@Entity
@Table(
  name = "oauth2_provider_accesstoken",
  schema = "public",
  indexes = [
    Index(
      name = "oauth2_provider_accesstoken_application_id_b22886e1",
      columnList = "application_id",
    ),
    Index(
      name = "oauth2_provider_accesstoken_user_id_6e4c9a65",
      columnList = "user_id",
    ),
    Index(
      name = "oauth2_provider_accesstoken_token_checksum_85319a26_like",
      columnList = "token_checksum",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "oauth2_provider_accesstoken_source_refresh_token_id_key",
      columnNames = ["source_refresh_token_id"],
    ),
    UniqueConstraint(
      name = "oauth2_provider_accesstoken_id_token_id_key",
      columnNames = ["id_token_id"],
    ),
    UniqueConstraint(
      name = "oauth2_provider_accesstoken_token_checksum_85319a26_uniq",
      columnNames = ["token_checksum"],
    ),
  ],
)
open class Oauth2ProviderAccesstoken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long = 0L

  @NotNull
  @Column(name = "token", nullable = false, length = Integer.MAX_VALUE)
  open var token: String = ""

  @NotNull
  @Column(name = "expires", nullable = false)
  open var expires: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "scope", nullable = false, length = Integer.MAX_VALUE)
  open var scope: String = ""

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  open var application: Oauth2ProviderApplication? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  open var user: AuthUser? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "updated", nullable = false)
  open var updated: OffsetDateTime = OffsetDateTime.now()

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_refresh_token_id")
  open var sourceRefreshToken: Oauth2ProviderRefreshtoken? =
    null

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_token_id")
  open var idToken: Oauth2ProviderIdtoken? = null

  @Size(max = 64)
  @NotNull
  @Column(name = "token_checksum", nullable = false, length = 64)
  open var tokenChecksum: String = ""
}
