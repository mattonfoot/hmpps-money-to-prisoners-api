package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(hidden = true)
data class PatchCheckRequest(
  @JsonProperty("assigned_to")
  val assignedTo: String?,
)
