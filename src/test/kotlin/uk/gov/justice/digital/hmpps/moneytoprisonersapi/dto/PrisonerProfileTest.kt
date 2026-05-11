package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrison
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit as CreditEntity
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement as DisbursementEntity
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile as PrisonerProfileEntity

@DisplayName("PrisonerProfile")
class PrisonerProfileTest {

  private fun profile(prisonerNumber: String = "A1234BC") = PrisonerProfileEntity().apply {
    this.prisonerNumber = prisonerNumber
  }

  private fun credit(amount: Long = 100, dob: LocalDate? = null) = CreditEntity().apply {
    this.amount = amount
    this.prisonerDob = dob
    this.resolution = CreditResolution.CREDITED.value
  }

  private fun prison(nomisId: String) = PrisonPrison().apply { this.nomisId = nomisId }

  @Test
  fun `prisonerDob is taken from the most common dob across credits`() {
    val p = profile()
    val dob = LocalDate.of(1990, 5, 15)
    p.credits = mutableListOf(credit(100, dob), credit(200, dob))

    val dto = PrisonerProfile.from(p, disbursements = emptyList())

    assertThat(dto.prisonerDob).isEqualTo(dob)
  }

  @Test
  fun `disbursementCount and disbursementTotal are computed from disbursements list`() {
    val p = profile()
    val disbursements = listOf(
      DisbursementEntity().apply {
        amount = 5000
        method = DisbursementMethod.BANK_TRANSFER.value
      },
      DisbursementEntity().apply {
        amount = 3000
        method = DisbursementMethod.CHEQUE.value
      },
    )

    val dto = PrisonerProfile.from(p, disbursements = disbursements)

    assertThat(dto.disbursementCount).isEqualTo(2)
    assertThat(dto.disbursementTotal).isEqualTo(8000)
  }

  @Test
  fun `currentPrison is the most recent credit's prison`() {
    val p = profile()
    val c1 = credit(100).apply {
      prison = prison("LEI")
      created = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    }
    val c2 = credit(200).apply {
      prison = prison("MDI")
      created = OffsetDateTime.of(2024, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    }
    p.credits = mutableListOf(c1, c2)

    val dto = PrisonerProfile.from(p, disbursements = emptyList())

    assertThat(dto.currentPrison).isEqualTo("MDI")
  }

  @Test
  fun `senderCount counts unique sender profiles linked to credits`() {
    val p = profile()
    val dto = PrisonerProfile.from(p, disbursements = emptyList(), senderCount = 3)

    assertThat(dto.senderCount).isEqualTo(3)
  }

  @Test
  fun `recipientCount counts unique recipient profiles linked via disbursements`() {
    val p = profile()
    val dto = PrisonerProfile.from(p, disbursements = emptyList(), recipientCount = 2)

    assertThat(dto.recipientCount).isEqualTo(2)
  }
}
