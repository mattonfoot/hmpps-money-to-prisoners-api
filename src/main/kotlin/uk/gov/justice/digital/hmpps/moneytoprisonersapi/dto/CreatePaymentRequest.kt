package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Request body for creating a new payment")
data class CreatePaymentRequest(
  @Schema(description = "Prisoner number (NOMIS ID)", example = "A1234BC")
  @JsonProperty("prisoner_number")
  val prisonerNumber: String?,

  @Schema(description = "Prisoner date of birth", example = "1990-01-15")
  @JsonProperty("prisoner_dob")
  val prisonerDob: LocalDate?,

  @Schema(description = "Amount in pence", example = "5000")
  val amount: Long?,

  @Schema(description = "IP address of the sender", example = "192.168.1.1")
  @JsonProperty("ip_address")
  val ipAddress: String? = null,
)
