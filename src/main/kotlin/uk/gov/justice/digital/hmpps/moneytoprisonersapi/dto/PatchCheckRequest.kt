package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PatchCheckRequest(
  @JsonProperty("assigned_to")
  val assignedTo: String?,
)
