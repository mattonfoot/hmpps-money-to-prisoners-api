package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerCreditNoticeEmail
import java.time.LocalDateTime

@Schema(description = "A prisoner credit notice email configuration")
data class PrisonerCreditNoticeEmailDto(
  @Schema(description = "Prison NOMIS ID", example = "LEI")
  val prison: String,

  @Schema(description = "Email address for credit notices", example = "clerk@prison.gov.uk")
  val email: String,

  @Schema(description = "Timestamp when the record was created")
  val created: LocalDateTime?,

  @Schema(description = "Timestamp when the record was last modified")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(email: PrisonerCreditNoticeEmail): PrisonerCreditNoticeEmailDto = PrisonerCreditNoticeEmailDto(
      prison = email.prison.nomisId,
      email = email.email,
      created = email.created,
      modified = email.modified,
    )
  }
}
