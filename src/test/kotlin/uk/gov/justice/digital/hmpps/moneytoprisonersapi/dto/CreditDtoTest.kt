package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("CreditDto")
class CreditDtoTest {

  private fun createCredit(
    id: Long? = 1L,
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prisonerName: String? = "John Smith",
    prisonerDob: LocalDate? = LocalDate.of(1990, 1, 15),
    prison: String? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    reviewed: Boolean = false,
    reconciled: Boolean = false,
    receivedAt: LocalDateTime? = LocalDateTime.of(2024, 3, 15, 10, 30),
    owner: String? = null,
    source: CreditSource = CreditSource.UNKNOWN,
    incompleteSenderInfo: Boolean = false,
  ): Credit {
    val credit = Credit(
      id = id,
      amount = amount,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      prisonerDob = prisonerDob,
      prison = prison,
      resolution = resolution,
      blocked = blocked,
      reviewed = reviewed,
      reconciled = reconciled,
      receivedAt = receivedAt,
      owner = owner,
      incompleteSenderInfo = incompleteSenderInfo,
    )
    credit.source = source
    credit.onCreate()
    return credit
  }

  @Nested
  @DisplayName("CRD-015: credit_pending status in DTO")
  inner class CreditPendingDto {

    @Test
    fun `status is credit_pending when prison set, pending, not blocked`() {
      val credit = createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = false)
      val dto = CreditDto.from(credit)
      assertThat(dto.status).isEqualTo(CreditStatus.CREDIT_PENDING)
    }

    @Test
    fun `status is credit_pending when prison set, manual, not blocked`() {
      val credit = createCredit(prison = "LEI", resolution = CreditResolution.MANUAL, blocked = false)
      val dto = CreditDto.from(credit)
      assertThat(dto.status).isEqualTo(CreditStatus.CREDIT_PENDING)
    }
  }

  @Nested
  @DisplayName("CRD-016: credited status in DTO")
  inner class CreditedDto {

    @Test
    fun `status is credited when resolution is credited`() {
      val credit = createCredit(resolution = CreditResolution.CREDITED)
      val dto = CreditDto.from(credit)
      assertThat(dto.status).isEqualTo(CreditStatus.CREDITED)
    }
  }

  @Nested
  @DisplayName("CRD-017: refund_pending status in DTO")
  inner class RefundPendingDto {

    @Test
    fun `status is refund_pending when no prison, pending, sender info complete`() {
      val credit = createCredit(prison = null, resolution = CreditResolution.PENDING, incompleteSenderInfo = false)
      val dto = CreditDto.from(credit)
      assertThat(dto.status).isEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `status is refund_pending when blocked, pending, sender info complete`() {
      val credit = createCredit(prison = "LEI", resolution = CreditResolution.PENDING, blocked = true, incompleteSenderInfo = false)
      val dto = CreditDto.from(credit)
      assertThat(dto.status).isEqualTo(CreditStatus.REFUND_PENDING)
    }

    @Test
    fun `status is NOT refund_pending when incomplete sender info`() {
      val credit = createCredit(prison = null, resolution = CreditResolution.PENDING, incompleteSenderInfo = true)
      val dto = CreditDto.from(credit)
      assertThat(dto.status).isNotEqualTo(CreditStatus.REFUND_PENDING)
    }
  }

  @Nested
  @DisplayName("CRD-018: refunded status in DTO")
  inner class RefundedDto {

    @Test
    fun `status is refunded when resolution is refunded`() {
      val credit = createCredit(resolution = CreditResolution.REFUNDED)
      val dto = CreditDto.from(credit)
      assertThat(dto.status).isEqualTo(CreditStatus.REFUNDED)
    }
  }

  @Nested
  @DisplayName("CRD-019: failed status in DTO")
  inner class FailedDto {

    @Test
    fun `status is failed when resolution is failed`() {
      val credit = createCredit(resolution = CreditResolution.FAILED)
      val dto = CreditDto.from(credit)
      assertThat(dto.status).isEqualTo(CreditStatus.FAILED)
    }
  }

  @Nested
  @DisplayName("DTO field mapping")
  inner class FieldMapping {

    @Test
    fun `maps all fields from Credit entity`() {
      val credit = createCredit(
        id = 42L,
        amount = 5000,
        prisonerNumber = "B5678DE",
        prisonerName = "Jane Doe",
        prisonerDob = LocalDate.of(1985, 6, 20),
        prison = "LEI",
        resolution = CreditResolution.CREDITED,
        blocked = false,
        reviewed = true,
        reconciled = true,
        receivedAt = LocalDateTime.of(2024, 3, 15, 10, 30),
        owner = "clerk1",
        source = CreditSource.BANK_TRANSFER,
      )
      val dto = CreditDto.from(credit)

      assertThat(dto.id).isEqualTo(42L)
      assertThat(dto.amount).isEqualTo(5000)
      assertThat(dto.prisonerNumber).isEqualTo("B5678DE")
      assertThat(dto.prisonerName).isEqualTo("Jane Doe")
      assertThat(dto.prisonerDob).isEqualTo(LocalDate.of(1985, 6, 20))
      assertThat(dto.prison).isEqualTo("LEI")
      assertThat(dto.resolution).isEqualTo(CreditResolution.CREDITED)
      assertThat(dto.source).isEqualTo(CreditSource.BANK_TRANSFER)
      assertThat(dto.owner).isEqualTo("clerk1")
      assertThat(dto.blocked).isFalse()
      assertThat(dto.reviewed).isTrue()
      assertThat(dto.reconciled).isTrue()
      assertThat(dto.receivedAt).isEqualTo(LocalDateTime.of(2024, 3, 15, 10, 30))
      assertThat(dto.created).isNotNull()
      assertThat(dto.modified).isNotNull()
      assertThat(dto.status).isEqualTo(CreditStatus.CREDITED)
    }

    @Test
    fun `handles null fields correctly`() {
      val credit = createCredit(
        prisonerNumber = null,
        prisonerName = null,
        prisonerDob = null,
        prison = null,
        receivedAt = null,
        owner = null,
      )
      val dto = CreditDto.from(credit)

      assertThat(dto.prisonerNumber).isNull()
      assertThat(dto.prisonerName).isNull()
      assertThat(dto.prisonerDob).isNull()
      assertThat(dto.prison).isNull()
      assertThat(dto.receivedAt).isNull()
      assertThat(dto.owner).isNull()
    }
  }
}
