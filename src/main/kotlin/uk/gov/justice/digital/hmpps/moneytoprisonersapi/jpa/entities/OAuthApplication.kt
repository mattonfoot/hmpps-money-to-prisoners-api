package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "oauth2_provider_application")
class OAuthApplication(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "client_id", nullable = false, unique = true, length = 100)
  val clientId: String = "",

  @Column(name = "client_secret", nullable = false, length = 255)
  val clientSecret: String = "",

  @Column(nullable = false, length = 255)
  val name: String = "",

  @Column(name = "client_type", nullable = false, length = 32)
  val clientType: String = "confidential",

  @Column(name = "authorization_grant_type", nullable = false, length = 32)
  val authorizationGrantType: String = "password",
)
