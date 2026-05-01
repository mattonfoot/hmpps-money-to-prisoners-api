package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Short user representation used in nested references where only username + name are needed.
 * Mirrors Python's `Basic User` schema.
 */
@Schema(name = "Basic User", description = "Short user representation")
data class BasicUser(
  @Schema(description = "Username", required = true)
  val username: String,
  @JsonProperty("first_name")
  val firstName: String? = null,
  @JsonProperty("last_name")
  val lastName: String? = null,
)
