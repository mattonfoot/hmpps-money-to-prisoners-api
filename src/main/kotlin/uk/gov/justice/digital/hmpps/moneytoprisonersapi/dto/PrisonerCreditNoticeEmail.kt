package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "A prisoner credit notice email configuration")
data class PrisonerCreditNoticeEmail(
  @Schema(description = "Prison NOMIS ID", example = "LEI")
  val prison: String,

  @Schema(description = "Email address for credit notices", example = "clerk@prison.gov.uk")
  val email: String,

  @Schema(description = "Timestamp when the record was created")
  val created: OffsetDateTime?,

  @Schema(description = "Timestamp when the record was last modified")
  val modified: OffsetDateTime?,
) {
  companion object {
    fun from(email: uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerCreditNoticeEmail): PrisonerCreditNoticeEmail = PrisonerCreditNoticeEmail(
      prison = email.prison.nomisId,
      email = email.email,
      created = email.created,
      modified = email.modified,
    )
  }
}
