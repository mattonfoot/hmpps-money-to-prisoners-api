package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus.Companion.computeFrom
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "A credit record representing money sent to a prisoner")
data class CreditDto(
  @Schema(description = "Unique identifier", example = "1")
  val id: Long?,
  @Schema(description = "Amount in pence", example = "5000")
  val amount: Long,
  @Schema(description = "Prisoner number (NOMIS ID)", example = "A1234BC")
  val prisonerNumber: String?,
  @Schema(description = "Prisoner full name", example = "John Smith")
  val prisonerName: String?,
  @Schema(description = "Prisoner date of birth", example = "1990-01-15")
  val prisonerDob: LocalDate?,
  @Schema(description = "Prison NOMIS ID where prisoner is located", example = "LEI")
  val prison: String?,
  @Schema(description = "Resolution status of the credit", example = "PENDING")
  val resolution: CreditResolution,
  @Schema(description = "Source type of the credit", example = "BANK_TRANSFER")
  val source: CreditSource,
  @Schema(description = "Computed display status derived from resolution, prison assignment, and blocked state", example = "credit_pending")
  val status: CreditStatus,
  @Schema(description = "Username of the clerk who credited the prisoner", example = "clerk1")
  val owner: String?,
  @Schema(description = "Whether the credit is blocked", example = "false")
  val blocked: Boolean,
  @Schema(description = "Whether the credit has been reviewed by security staff", example = "false")
  val reviewed: Boolean,
  @Schema(description = "Whether the credit has been reconciled", example = "false")
  val reconciled: Boolean,
  @Schema(description = "Timestamp when the credit was received", example = "2024-03-15T10:30:00")
  val receivedAt: LocalDateTime?,
  @Schema(description = "Timestamp when the record was created", example = "2024-03-15T10:30:00")
  val created: LocalDateTime?,
  @Schema(description = "Timestamp when the record was last modified", example = "2024-03-15T10:30:00")
  val modified: LocalDateTime?,
) {
  companion object {
    fun from(credit: Credit): CreditDto = CreditDto(
      id = credit.id,
      amount = credit.amount,
      prisonerNumber = credit.prisonerNumber,
      prisonerName = credit.prisonerName,
      prisonerDob = credit.prisonerDob,
      prison = credit.prison,
      resolution = credit.resolution,
      source = credit.source,
      status = computeFrom(credit),
      owner = credit.owner,
      blocked = credit.blocked,
      reviewed = credit.reviewed,
      reconciled = credit.reconciled,
      receivedAt = credit.receivedAt,
      created = credit.created,
      modified = credit.modified,
    )
  }
}
