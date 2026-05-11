package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import java.time.OffsetDateTime
import java.time.ZoneOffset
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit as CreditEntity

@DisplayName("SecurityCheckCreditDto")
class SecurityCheckCreditDtoTest {

  private fun createCredit(
    id: Long? = 1L,
    amount: Long = 1000,
    prisonNomisId: String? = "LEI",
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    source: CreditSource = CreditSource.BANK_TRANSFER,
  ): CreditEntity = CreditEntity().apply {
    this.id = id
    this.amount = amount
    this.prison = prisonNomisId?.let { nomis -> PrisonPrison().apply { this.nomisId = nomis } }
    this.resolution = resolution.value
    this.blocked = blocked
    this.source = source
  }

  @Nested
  @DisplayName("CRD-108: Security check serializer adds check object")
  inner class CheckObject {

    @Test
    fun `includes nested security check when present`() {
      val credit = createCredit()
      val securityCheck = SecurityCheck().apply {
        id = 10L
        status = CheckStatus.ACCEPTED.value
        description = arrayOf("Verified sender")
        decisionReason = "Known sender"
        actionedAt = OffsetDateTime.of(2024, 3, 16, 14, 0, 0, 0, ZoneOffset.UTC)
      }
      securityCheck.credit = credit
      credit.securityCheck = securityCheck

      val dto = SecurityCheckCreditDto.from(credit)
      assertThat(dto.securityCheck).isNotNull
      assertThat(dto.securityCheck!!.id).isEqualTo(10L)
      assertThat(dto.securityCheck!!.status).isEqualTo(CheckStatus.ACCEPTED)
      assertThat(dto.securityCheck!!.description).isNotNull()
      assertThat(dto.securityCheck!!.decisionReason).isEqualTo("Known sender")
      assertThat(dto.securityCheck!!.actionedAt).isEqualTo(
        OffsetDateTime.of(2024, 3, 16, 14, 0, 0, 0, ZoneOffset.UTC),
      )
    }

    @Test
    fun `security check is null when not present`() {
      val credit = createCredit()
      val dto = SecurityCheckCreditDto.from(credit)
      assertThat(dto.securityCheck).isNull()
    }

    @Test
    fun `includes pending security check`() {
      val credit = createCredit()
      val securityCheck = SecurityCheck().apply {
        id = 11L
        status = CheckStatus.PENDING.value
      }
      securityCheck.credit = credit
      credit.securityCheck = securityCheck

      val dto = SecurityCheckCreditDto.from(credit)
      assertThat(dto.securityCheck).isNotNull
      assertThat(dto.securityCheck!!.status).isEqualTo(CheckStatus.PENDING)
      assertThat(dto.securityCheck!!.actionedBy).isNull()
      assertThat(dto.securityCheck!!.actionedAt).isNull()
    }

    @Test
    fun `includes rejected security check`() {
      val credit = createCredit()
      val securityCheck = SecurityCheck().apply {
        id = 12L
        status = CheckStatus.REJECTED.value
        decisionReason = "Suspicious activity"
        actionedAt = OffsetDateTime.of(2024, 3, 17, 10, 0, 0, 0, ZoneOffset.UTC)
      }
      securityCheck.credit = credit
      credit.securityCheck = securityCheck

      val dto = SecurityCheckCreditDto.from(credit)
      assertThat(dto.securityCheck!!.status).isEqualTo(CheckStatus.REJECTED)
      assertThat(dto.securityCheck!!.decisionReason).isEqualTo("Suspicious activity")
    }
  }

  @Nested
  @DisplayName("Inherits SecurityCreditDto fields")
  inner class InheritsSecurityFields {

    @Test
    fun `includes all base and security fields`() {
      val credit = createCredit(id = 42L, amount = 5000)
      val dto = SecurityCheckCreditDto.from(credit, senderProfileId = 10L, prisonerProfileId = 20L)

      assertThat(dto.id).isEqualTo(42L)
      assertThat(dto.amount).isEqualTo(5000)
      assertThat(dto.senderProfile).isEqualTo(10L)
      assertThat(dto.prisonerProfile).isEqualTo(20L)
    }
  }
}
