package uk.gov.justice.digital.hmpps.moneytoprisonersapi.security

import org.springframework.http.HttpMethod

/**
 * Maps HTTP methods to logical permission levels, mirroring the Django REST Framework
 * `ActionsBasedPermissions` behaviour (COR-020).
 *
 * | HTTP Method | Permission |
 * |-------------|-----------|
 * | POST        | add       |
 * | PUT / PATCH | change    |
 * | DELETE      | delete    |
 * | GET / HEAD  | null (authentication only) |
 */
object ActionsBasedPermissions {

  /**
   * Returns the permission name required for the given HTTP method,
   * or null if the method requires no explicit permission beyond authentication.
   */
  fun permissionFor(method: HttpMethod): String? = when (method) {
    HttpMethod.POST -> "add"
    HttpMethod.PUT, HttpMethod.PATCH -> "change"
    HttpMethod.DELETE -> "delete"
    else -> null
  }
}
