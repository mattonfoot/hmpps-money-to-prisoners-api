package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "oauth2_provider_accesstoken")
class OAuthAccessToken(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(nullable = false, unique = true, length = 255)
  val token: String = "",

  @Column(nullable = false)
  val expires: OffsetDateTime = OffsetDateTime.now(),

  @Column(nullable = false)
  val scope: String = "",

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "application_id")
  val application: OAuthApplication? = null,

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id")
  val user: AuthUser? = null,
)
