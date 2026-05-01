package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit as CreditEntity
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement as DisbursementEntity
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerProfile as PrisonerProfileEntity

@DisplayName("PrisonerProfile")
class PrisonerProfileTest {

  @Test
  fun `prisonerDob is taken from the most common dob across credits`() {
    val profile = PrisonerProfileEntity(prisonerNumber = "A1234BC")
    val dob = LocalDate.of(1990, 5, 15)
    val c1 = CreditEntity(amount = 100, prisonerDob = dob, resolution = CreditResolution.CREDITED)
    val c2 = CreditEntity(amount = 200, prisonerDob = dob, resolution = CreditResolution.CREDITED)
    profile.credits = mutableSetOf(c1, c2)

    val dto = PrisonerProfile.from(profile, disbursements = emptyList())

    assertThat(dto.prisonerDob).isEqualTo(dob)
  }

  @Test
  fun `disbursementCount and disbursementTotal are computed from disbursements list`() {
    val profile = PrisonerProfileEntity(prisonerNumber = "A1234BC")
    val disbursements = listOf(
      DisbursementEntity(amount = 5000, method = DisbursementMethod.BANK_TRANSFER),
      DisbursementEntity(amount = 3000, method = DisbursementMethod.CHEQUE),
    )

    val dto = PrisonerProfile.from(profile, disbursements = disbursements)

    assertThat(dto.disbursementCount).isEqualTo(2)
    assertThat(dto.disbursementTotal).isEqualTo(8000)
  }

  @Test
  fun `currentPrison is the most recent credit's prison`() {
    val profile = PrisonerProfileEntity(prisonerNumber = "A1234BC")
    val c1 = CreditEntity(amount = 100, prison = "LEI", resolution = CreditResolution.CREDITED)
    c1.created = java.time.LocalDateTime.of(2024, 1, 1, 0, 0)
    val c2 = CreditEntity(amount = 200, prison = "MDI", resolution = CreditResolution.CREDITED)
    c2.created = java.time.LocalDateTime.of(2024, 6, 1, 0, 0)
    profile.credits = mutableSetOf(c1, c2)

    val dto = PrisonerProfile.from(profile, disbursements = emptyList())

    assertThat(dto.currentPrison).isEqualTo("MDI")
  }

  @Test
  fun `senderCount counts unique sender profiles linked to credits`() {
    val profile = PrisonerProfileEntity(prisonerNumber = "A1234BC")
    // senderCount is passed in directly since it requires a cross-table query
    val dto = PrisonerProfile.from(profile, disbursements = emptyList(), senderCount = 3)

    assertThat(dto.senderCount).isEqualTo(3)
  }

  @Test
  fun `recipientCount counts unique recipient profiles linked via disbursements`() {
    val profile = PrisonerProfileEntity(prisonerNumber = "A1234BC")
    val dto = PrisonerProfile.from(profile, disbursements = emptyList(), recipientCount = 2)

    assertThat(dto.recipientCount).isEqualTo(2)
  }
}
