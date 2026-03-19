package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdatePrisonRequest(
  @JsonProperty("prisoner_number")
  val prisonerNumber: String,

  @JsonProperty("prison")
  val prison: String,
)
