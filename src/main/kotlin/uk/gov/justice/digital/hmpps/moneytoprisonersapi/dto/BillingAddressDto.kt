package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.BillingAddress

@Schema(description = "Billing address associated with a payment")
data class BillingAddressDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,
  @Schema(description = "Address line 1", example = "10 Downing Street")
  val line1: String?,
  @Schema(description = "Address line 2", example = "Flat 2")
  val line2: String?,
  @Schema(description = "City", example = "London")
  val city: String?,
  @Schema(description = "Country code", example = "GB")
  val country: String?,
  @Schema(description = "Postcode", example = "SW1A 2AA")
  val postcode: String?,
) {
  companion object {
    fun from(billingAddress: BillingAddress): BillingAddressDto = BillingAddressDto(
      id = billingAddress.id,
      line1 = billingAddress.line1,
      line2 = billingAddress.line2,
      city = billingAddress.city,
      country = billingAddress.country,
      postcode = billingAddress.postcode,
    )
  }
}
