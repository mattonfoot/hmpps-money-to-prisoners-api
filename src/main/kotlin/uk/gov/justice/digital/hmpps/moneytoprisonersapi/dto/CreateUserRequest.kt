package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(hidden = true)
data class CreateUserRequest(
  @Schema(description = "Username (unique, case-insensitive)", required = true)
  val username: String?,

  @Schema(description = "Email address", required = true)
  val email: String?,

  @Schema(description = "First name")
  @JsonProperty("first_name")
  val firstName: String? = null,

  @Schema(description = "Last name")
  @JsonProperty("last_name")
  val lastName: String? = null,

  @Schema(description = "Role name to assign")
  @JsonProperty("role")
  val roleName: String? = null,

  @Schema(description = "Prisons to assign to the user. Accepts either NOMIS ID strings or {nomis_id} objects.")
  @JsonProperty("prisons")
  val prisonsRaw: List<Any>? = null,
) {
  /**
   * Normalises `prisons` to a list of NOMIS IDs. Accepts either:
   *   * `["IXB", "BWI"]` — list of strings (Kotlin-native shorthand), or
   *   * `[{"nomis_id": "IXB"}, ...]` — list of objects (Python's UserSerializer shape).
   */
  val prisonIds: List<String>?
    get() = prisonsRaw?.mapNotNull { element ->
      when (element) {
        is String -> element
        is Map<*, *> -> (element["nomis_id"] as? String)
        else -> null
      }
    }
}
