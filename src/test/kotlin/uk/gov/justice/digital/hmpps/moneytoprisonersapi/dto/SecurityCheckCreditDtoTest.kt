package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.SecurityCheck
import java.time.LocalDateTime

@DisplayName("SecurityCheckCreditDto")
class SecurityCheckCreditDtoTest {

  private fun createCredit(
    id: Long? = 1L,
    amount: Long = 1000,
    prison: String? = "LEI",
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    source: CreditSource = CreditSource.BANK_TRANSFER,
  ): Credit {
    val credit = Credit(
      id = id,
      amount = amount,
      prison = prison,
      resolution = resolution,
      blocked = blocked,
    )
    credit.source = source
    credit.onCreate()
    return credit
  }

  @Nested
  @DisplayName("CRD-108: Security check serializer adds check object")
  inner class CheckObject {

    @Test
    fun `includes nested security check when present`() {
      val credit = createCredit()
      val securityCheck = SecurityCheck(
        id = 10L,
        status = CheckStatus.ACCEPTED,
        description = "Verified sender",
        decisionReason = "Known sender",
        actionedBy = "security_user",
        actionedAt = LocalDateTime.of(2024, 3, 16, 14, 0),
      )
      securityCheck.credit = credit
      securityCheck.onCreate()
      credit.securityCheck = securityCheck

      val dto = SecurityCheckCreditDto.from(credit)
      assertThat(dto.securityCheck).isNotNull
      assertThat(dto.securityCheck!!.id).isEqualTo(10L)
      assertThat(dto.securityCheck!!.status).isEqualTo(CheckStatus.ACCEPTED)
      assertThat(dto.securityCheck!!.description).isEqualTo("Verified sender")
      assertThat(dto.securityCheck!!.decisionReason).isEqualTo("Known sender")
      assertThat(dto.securityCheck!!.actionedBy).isEqualTo("security_user")
      assertThat(dto.securityCheck!!.actionedAt).isEqualTo(LocalDateTime.of(2024, 3, 16, 14, 0))
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
      val securityCheck = SecurityCheck(
        id = 11L,
        status = CheckStatus.PENDING,
      )
      securityCheck.credit = credit
      securityCheck.onCreate()
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
      val securityCheck = SecurityCheck(
        id = 12L,
        status = CheckStatus.REJECTED,
        decisionReason = "Suspicious activity",
        actionedBy = "security_admin",
        actionedAt = LocalDateTime.of(2024, 3, 17, 10, 0),
      )
      securityCheck.credit = credit
      securityCheck.onCreate()
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
