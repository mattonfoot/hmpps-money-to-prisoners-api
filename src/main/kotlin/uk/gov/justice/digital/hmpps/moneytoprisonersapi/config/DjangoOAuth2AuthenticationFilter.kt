package uk.gov.justice.digital.hmpps.moneytoprisonersapi.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AuthUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.OAuthAccessToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.OAuthAccessTokenRepository
import java.time.OffsetDateTime

/**
 * Maps Django auth groups to Spring Security granted authorities.
 * This mapping ensures @PreAuthorize annotations work with Django's group-based permissions.
 */
private val GROUP_TO_AUTHORITY = mapOf(
  "BankAdmin" to "ROLE_BANK_ADMIN",
  "PrisonClerk" to "ROLE_PRISON_CLERK",
  "Security" to "ROLE_SECURITY_STAFF",
  "FIU" to "ROLE_FIU",
  "SendMoney" to "ROLE_SEND_MONEY",
  "PrisonerLocationAdmin" to "ROLE_NOMS_OPS",
  "RefundBankAdmin" to "ROLE_REFUND_BANK_ADMIN",
  "DisbursementBankAdmin" to "ROLE_DISBURSEMENT_BANK_ADMIN",
  "UserAdmin" to "ROLE_USER_ADMIN",
)

/**
 * Some Django groups grant additional Spring authorities beyond the primary mapping.
 * PrisonClerk users also get ROLE_CASHBOOK since the cashbook application
 * uses the PrisonClerk group for its access control.
 */
private val GROUP_TO_EXTRA_AUTHORITY = mapOf(
  "PrisonClerk" to "ROLE_CASHBOOK",
)

/**
 * Authentication token that carries the Django OAuth2 user principal,
 * their Spring authorities (mapped from Django groups), and the OAuth2 client_id.
 */
class DjangoOAuth2Authentication(
  private val username: String,
  val clientId: String?,
  authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
  init {
    isAuthenticated = true
  }

  override fun getCredentials(): Any? = null
  override fun getPrincipal(): Any = username
  override fun getName(): String = username
}

/**
 * Spring Security filter that validates Bearer tokens against the
 * `oauth2_provider_accesstoken` database table (Django OAuth Toolkit compatible).
 *
 * Replaces HMPPS Auth JWT validation so both Python and Kotlin APIs
 * authenticate against the same database.
 */
@Component
class DjangoOAuth2AuthenticationFilter(
  private val tokenRepository: OAuthAccessTokenRepository,
) : OncePerRequestFilter() {

  private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val token = extractBearerToken(request)
    log.info("DjangoAuth: ${request.method} ${request.requestURI} token=${token?.take(10)}...")

    if (token != null) {
      try {
        val accessToken = tokenRepository.findByToken(token)
        log.info("DjangoAuth: lookup returned ${if (accessToken == null) "null" else "id=${accessToken.id}"}")
        if (accessToken != null && !isExpired(accessToken)) {
          val authentication = buildAuthentication(accessToken)
          SecurityContextHolder.getContext().authentication = authentication
          log.info("DjangoAuth: authenticated as ${authentication.name} with ${authentication.authorities}")
        } else {
          log.warn("DjangoAuth: token not found or expired")
        }
      } catch (e: Exception) {
        log.error("DjangoAuth: error during token lookup", e)
      }
    }

    filterChain.doFilter(request, response)
  }

  private fun extractBearerToken(request: HttpServletRequest): String? {
    val header = request.getHeader("Authorization") ?: return null
    if (!header.startsWith("Bearer ", ignoreCase = true)) return null
    return header.substring(7).trim()
  }

  private fun isExpired(token: OAuthAccessToken): Boolean = token.expires.isBefore(OffsetDateTime.now())

  private fun buildAuthentication(token: OAuthAccessToken): DjangoOAuth2Authentication {
    val user = token.user
    val authorities = buildAuthorities(user)

    return DjangoOAuth2Authentication(
      username = user?.username ?: "anonymous",
      clientId = token.application?.clientId,
      authorities = authorities,
    )
  }

  private fun buildAuthorities(user: AuthUser?): List<GrantedAuthority> {
    if (user == null) return emptyList()

    val authorities = mutableListOf<GrantedAuthority>()

    // Map Django groups to Spring authorities
    for (group in user.groups) {
      val authority = GROUP_TO_AUTHORITY[group.name]
      if (authority != null) {
        authorities.add(SimpleGrantedAuthority(authority))
      }
      // PrisonClerk group also grants ROLE_CASHBOOK (the cashbook app's role)
      val extraAuthority = GROUP_TO_EXTRA_AUTHORITY[group.name]
      if (extraAuthority != null) {
        authorities.add(SimpleGrantedAuthority(extraAuthority))
      }
    }

    return authorities
  }
}
