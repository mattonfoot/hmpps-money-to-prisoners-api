package uk.gov.justice.digital.hmpps.moneytoprisonersapi.security

/**
 * Represents the OAuth2 client applications that interact with this API.
 * Each value corresponds to the `client_id` claim in the JWT access token.
 *
 * COR-021 to COR-024.
 */
enum class ClientApplication(val clientId: String) {
  /** Prison cashbook application */
  CASHBOOK("cashbook"),

  /** NOMS Operations application */
  NOMS_OPS("noms-ops"),

  /** Bank admin application */
  BANK_ADMIN("bank-admin"),

  /** Send money (public-facing) application */
  SEND_MONEY("send-money"),
}
