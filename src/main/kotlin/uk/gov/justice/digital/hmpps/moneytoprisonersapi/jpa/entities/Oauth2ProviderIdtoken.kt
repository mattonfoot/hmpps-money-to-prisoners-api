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
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
  name = "oauth2_provider_idtoken",
  schema = "public",
  indexes = [
    Index(
      name = "oauth2_provider_idtoken_application_id_08c5ff4f",
      columnList = "application_id",
    ),
    Index(
      name = "oauth2_provider_idtoken_user_id_dd512b59",
      columnList = "user_id",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "oauth2_provider_idtoken_jti_key",
      columnNames = ["jti"],
    ),
  ],
)
open class Oauth2ProviderIdtoken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long = 0L

  @NotNull
  @Column(name = "jti", nullable = false)
  open var jti: UUID? = null

  @NotNull
  @Column(name = "expires", nullable = false)
  open var expires: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "scope", nullable = false, length = Integer.MAX_VALUE)
  open var scope: String = ""

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "updated", nullable = false)
  open var updated: OffsetDateTime = OffsetDateTime.now()

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  open var application: Oauth2ProviderApplication? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  open var user: AuthUser? = null
}
