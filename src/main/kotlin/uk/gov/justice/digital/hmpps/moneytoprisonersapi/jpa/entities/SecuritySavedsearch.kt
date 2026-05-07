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
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@Entity
@Table(
  name = "security_savedsearch",
  schema = "public",
  indexes = [
    Index(
      name = "security_savedsearch_user_id_a997de3d",
      columnList = "user_id",
    ),
  ],
)
open class SecuritySavedsearch {
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
  @Column(name = "description", nullable = false)
  open var description: String = ""

  @Size(max = 255)
  @NotNull
  @Column(name = "endpoint", nullable = false)
  open var endpoint: String = ""

  @NotNull
  @Column(name = "last_result_count", nullable = false)
  open var lastResultCount: Int = 0

  @Size(max = 1000)
  @Column(name = "site_url", length = 1000)
  open var siteUrl: String? = null

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  open var user: AuthUser? = null
}
