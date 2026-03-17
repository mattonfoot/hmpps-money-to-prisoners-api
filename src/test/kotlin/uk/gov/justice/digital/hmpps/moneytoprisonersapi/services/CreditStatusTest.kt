package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import java.time.LocalDate

@DisplayName("CreditStatus.computeFrom")
class CreditStatusTest {

  private fun createCredit(
    prison: String? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    incompleteSenderInfo: Boolean = false,
  ): Credit = Credit(
    amount = 1000,
    prisonerNumber = "A1234BC",
    prisonerName = "John Smith",
    prisonerDob = LocalDate.of(1990, 1, 15),
    prison = prison,
    resolution = resolution,
    blocked = blocked,
    incompleteSenderInfo = incompleteSenderInfo,
  )

  @Nested
  @DisplayName("CRD-015: credit_pending status")
  inner class CreditPending {

    @Test
    fun `prison set, pending, not blocked`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = "LEI", resolution = CreditResolution.PENDING)))
        .isEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `prison set, manual, not blocked`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = "LEI", resolution = CreditResolution.MANUAL)))
        .isEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `not credit_pending when blocked`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = true)))
        .isNotEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `not credit_pending when no prison`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = null, resolution = CreditResolution.PENDING)))
        .isNotEqualTo(CreditStatus.CREDIT_PENDING)
    }
  }

  @Nested
  @DisplayName("CRD-016: credited status")
  inner class Credited {

    @Test
    fun `resolution credited`() {
      assertThat(CreditStatus.computeFrom(createCredit(resolution = CreditResolution.CREDITED)))
        .isEqualTo(CreditStatus.CREDITED)
    }
  }

  @Nested
  @DisplayName("CRD-017: refund_pending status")
  inner class RefundPending {

    @Test
    fun `no prison, pending, sender info complete`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = null, resolution = CreditResolution.PENDING)))
        .isEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `blocked, pending, sender info complete`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = true)))
        .isEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `not refund_pending when incomplete sender info`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = null, resolution = CreditResolution.PENDING, incompleteSenderInfo = true)))
        .isNotEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `not refund_pending when blocked with incomplete sender info`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = "LEI", blocked = true, incompleteSenderInfo = true)))
        .isNotEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `not refund_pending when resolution is not pending`() {
      assertThat(CreditStatus.computeFrom(createCredit(prison = null, resolution = CreditResolution.MANUAL)))
        .isNotEqualTo(CreditStatus.REFUND_PENDING)
    }
  }

  @Nested
  @DisplayName("CRD-018: refunded status")
  inner class Refunded {

    @Test
    fun `resolution refunded`() {
      assertThat(CreditStatus.computeFrom(createCredit(resolution = CreditResolution.REFUNDED)))
        .isEqualTo(CreditStatus.REFUNDED)
    }
  }

  @Nested
  @DisplayName("CRD-019: failed status")
  inner class Failed {

    @Test
    fun `resolution failed`() {
      assertThat(CreditStatus.computeFrom(createCredit(resolution = CreditResolution.FAILED)))
        .isEqualTo(CreditStatus.FAILED)
    }
  }

  @Nested
  @DisplayName("Enum values")
  inner class EnumValues {

    @Test
    fun `all status values have correct string representation`() {
      assertThat(CreditStatus.INITIAL.value).isEqualTo("initial")
      assertThat(CreditStatus.CREDIT_PENDING.value).isEqualTo("credit_pending")
      assertThat(CreditStatus.CREDITED.value).isEqualTo("credited")
      assertThat(CreditStatus.REFUND_PENDING.value).isEqualTo("refund_pending")
      assertThat(CreditStatus.REFUNDED.value).isEqualTo("refunded")
      assertThat(CreditStatus.FAILED.value).isEqualTo("failed")
    }

    @Test
    fun `has exactly six values`() {
      assertThat(CreditStatus.entries).hasSize(6)
    }
  }
}
