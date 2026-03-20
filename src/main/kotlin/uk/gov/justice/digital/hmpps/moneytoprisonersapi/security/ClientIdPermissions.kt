package uk.gov.justice.digital.hmpps.moneytoprisonersapi.security

import org.springframework.security.core.Authentication

/**
 * Utility for checking OAuth2 client ID-based permissions.
 *
 * In HMPPS, each client application is identified by a `client_id` claim in the JWT.
 * This object provides methods to verify that the authenticated caller is from a
 * specific allowed application (COR-021 to COR-024).
 *
 * Usage in @PreAuthorize expressions:
 *   @PreAuthorize("@clientIdPermissions.isNomsOps(authentication)")
 */
object ClientIdPermissions {

  /**
   * Returns true if the authentication represents a token from the given [ClientApplication].
   * COR-021 to COR-024.
   */
  fun isAllowed(authentication: Authentication, application: ClientApplication): Boolean {
    val jwt = (authentication.principal as? org.springframework.security.oauth2.jwt.Jwt) ?: return false
    val tokenClientId = jwt.claims["client_id"] as? String ?: return false
    return tokenClientId == application.clientId
  }

  /**
   * COR-021: Returns true if the caller is the noms-ops client.
   */
  fun isNomsOps(authentication: Authentication): Boolean = isAllowed(authentication, ClientApplication.NOMS_OPS)

  /**
   * COR-022: Returns true if the caller is the send-money client.
   */
  fun isSendMoney(authentication: Authentication): Boolean = isAllowed(authentication, ClientApplication.SEND_MONEY)

  /**
   * COR-023: Returns true if the caller is the bank-admin client.
   */
  fun isBankAdmin(authentication: Authentication): Boolean = isAllowed(authentication, ClientApplication.BANK_ADMIN)

  /**
   * COR-024: Returns true if the caller is the cashbook client.
   */
  fun isCashbook(authentication: Authentication): Boolean = isAllowed(authentication, ClientApplication.CASHBOOK)
}
