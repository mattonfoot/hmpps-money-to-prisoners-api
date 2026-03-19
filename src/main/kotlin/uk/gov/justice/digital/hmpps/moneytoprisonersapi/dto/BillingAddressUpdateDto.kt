package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Billing address for update requests")
data class BillingAddressUpdateDto(
  @Schema(description = "Address line 1", example = "10 Downing Street")
  val line1: String? = null,

  @Schema(description = "Address line 2", example = "Flat 2")
  val line2: String? = null,

  @Schema(description = "City", example = "London")
  val city: String? = null,

  @Schema(description = "Country code", example = "GB")
  val country: String? = null,

  @Schema(description = "Postcode", example = "SW1A 2AA")
  val postcode: String? = null,
)
