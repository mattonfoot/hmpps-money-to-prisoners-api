package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.OAuthAccessToken

@Repository
interface OAuthAccessTokenRepository : JpaRepository<OAuthAccessToken, Long> {
  // Eager-fetch the user and their groups so the auth filter can read them
  // outside the JPA session.
  @EntityGraph(attributePaths = ["user", "user.groups", "application"])
  fun findByToken(token: String): OAuthAccessToken?
}
