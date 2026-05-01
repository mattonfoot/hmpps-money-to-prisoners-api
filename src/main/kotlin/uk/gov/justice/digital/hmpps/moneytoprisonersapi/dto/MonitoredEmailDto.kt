package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "MonitoredPartialEmailAddressSerialiser", description = "Request/response for a monitored partial email keyword")
data class MonitoredEmailDto(
  @Schema(description = "The email keyword to monitor (stored lowercase)", example = "fraud")
  val keyword: String,
)
