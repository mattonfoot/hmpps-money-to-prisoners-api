package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Comment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Log
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Payment
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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

  @Nested
  @DisplayName("CRD-100: Base serializer includes core fields")
  inner class CoreFields {

    @Test
    fun `includes sender_name from transaction`() {
      val credit = createCredit()
      val transaction = Transaction(amount = 1000)
      transaction.senderName = "Alice Sender"
      transaction.credit = credit
      credit.transaction = transaction

      val dto = CreditDto.from(credit)
      assertThat(dto.senderName).isEqualTo("Alice Sender")
    }

    @Test
    fun `sender_name is null when no transaction`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.senderName).isNull()
    }

    @Test
    fun `includes sender_email from payment`() {
      val credit = createCredit()
      val payment = Payment()
      payment.email = "alice@example.com"
      payment.credit = credit
      credit.payment = payment

      val dto = CreditDto.from(credit)
      assertThat(dto.senderEmail).isEqualTo("alice@example.com")
    }

    @Test
    fun `sender_email is null when no payment`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.senderEmail).isNull()
    }

    @Test
    fun `includes started_at mapped from created`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.startedAt).isEqualTo(credit.created)
    }

    @Test
    fun `includes owner_name as null by default`() {
      val credit = createCredit(owner = "clerk1")
      val dto = CreditDto.from(credit)
      assertThat(dto.ownerName).isNull()
    }
  }

  @Nested
  @DisplayName("CRD-101: Computed timestamps from log entries")
  inner class ComputedTimestamps {

    @Test
    fun `credited_at from CREDITED log entry`() {
      val credit = createCredit()
      val creditedTime = LocalDateTime.of(2024, 3, 16, 14, 0)
      val log = Log(action = LogAction.CREDITED)
      log.created = creditedTime
      log.credit = credit
      credit.logs.add(log)

      val dto = CreditDto.from(credit)
      assertThat(dto.creditedAt).isEqualTo(creditedTime)
    }

    @Test
    fun `credited_at is null when no CREDITED log`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.creditedAt).isNull()
    }

    @Test
    fun `refunded_at from REFUNDED log entry`() {
      val credit = createCredit()
      val refundedTime = LocalDateTime.of(2024, 3, 17, 9, 0)
      val log = Log(action = LogAction.REFUNDED)
      log.created = refundedTime
      log.credit = credit
      credit.logs.add(log)

      val dto = CreditDto.from(credit)
      assertThat(dto.refundedAt).isEqualTo(refundedTime)
    }

    @Test
    fun `refunded_at is null when no REFUNDED log`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.refundedAt).isNull()
    }

    @Test
    fun `set_manual_at from MANUAL log entry`() {
      val credit = createCredit()
      val manualTime = LocalDateTime.of(2024, 3, 18, 11, 0)
      val log = Log(action = LogAction.MANUAL)
      log.created = manualTime
      log.credit = credit
      credit.logs.add(log)

      val dto = CreditDto.from(credit)
      assertThat(dto.setManualAt).isEqualTo(manualTime)
    }

    @Test
    fun `set_manual_at is null when no MANUAL log`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.setManualAt).isNull()
    }
  }

  @Nested
  @DisplayName("CRD-102: short_payment_ref")
  inner class ShortPaymentRef {

    @Test
    fun `returns first 8 chars of payment UUID`() {
      val credit = createCredit()
      val uuid = UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890")
      val payment = Payment(uuid = uuid)
      payment.credit = credit
      credit.payment = payment

      val dto = CreditDto.from(credit)
      assertThat(dto.shortPaymentRef).isEqualTo("abcdef12")
    }

    @Test
    fun `is null when no payment`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.shortPaymentRef).isNull()
    }
  }

  @Nested
  @DisplayName("CRD-103: anonymous flag")
  inner class AnonymousFlag {

    @Test
    fun `anonymous is true when incomplete_sender_info and blocked`() {
      val credit = createCredit(incompleteSenderInfo = true, blocked = true)
      val transaction = Transaction(amount = 1000)
      transaction.incompleteSenderInfo = true
      transaction.credit = credit
      credit.transaction = transaction

      val dto = CreditDto.from(credit)
      assertThat(dto.anonymous).isTrue()
    }

    @Test
    fun `anonymous is false when not blocked`() {
      val credit = createCredit(incompleteSenderInfo = true, blocked = false)
      val transaction = Transaction(amount = 1000)
      transaction.incompleteSenderInfo = true
      transaction.credit = credit
      credit.transaction = transaction

      val dto = CreditDto.from(credit)
      assertThat(dto.anonymous).isFalse()
    }

    @Test
    fun `anonymous is false when sender info complete`() {
      val credit = createCredit(incompleteSenderInfo = false, blocked = true)
      val transaction = Transaction(amount = 1000)
      transaction.incompleteSenderInfo = false
      transaction.credit = credit
      credit.transaction = transaction

      val dto = CreditDto.from(credit)
      assertThat(dto.anonymous).isFalse()
    }

    @Test
    fun `anonymous is false when no transaction`() {
      val credit = createCredit(blocked = true, incompleteSenderInfo = true)
      val dto = CreditDto.from(credit)
      assertThat(dto.anonymous).isFalse()
    }
  }

  @Nested
  @DisplayName("CRD-104: intended_recipient")
  inner class IntendedRecipient {

    @Test
    fun `returns payment recipient_name`() {
      val credit = createCredit()
      val payment = Payment()
      payment.recipientName = "John Prisoner"
      payment.credit = credit
      credit.payment = payment

      val dto = CreditDto.from(credit)
      assertThat(dto.intendedRecipient).isEqualTo("John Prisoner")
    }

    @Test
    fun `is null when no payment`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.intendedRecipient).isNull()
    }

    @Test
    fun `is null when payment has no recipient_name`() {
      val credit = createCredit()
      val payment = Payment()
      payment.recipientName = null
      payment.credit = credit
      credit.payment = payment

      val dto = CreditDto.from(credit)
      assertThat(dto.intendedRecipient).isNull()
    }
  }

  @Nested
  @DisplayName("CRD-105: comments nested array")
  inner class CommentsNested {

    @Test
    fun `includes comments as nested DTOs`() {
      val credit = createCredit()
      val comment1 = Comment(id = 1L, comment = "First comment", userId = "user1")
      comment1.credit = credit
      comment1.onCreate()
      val comment2 = Comment(id = 2L, comment = "Second comment", userId = "user2")
      comment2.credit = credit
      comment2.onCreate()
      credit.comments.addAll(listOf(comment1, comment2))

      val dto = CreditDto.from(credit)
      assertThat(dto.comments).hasSize(2)
      assertThat(dto.comments[0].comment).isEqualTo("First comment")
      assertThat(dto.comments[0].userId).isEqualTo("user1")
      assertThat(dto.comments[1].comment).isEqualTo("Second comment")
    }

    @Test
    fun `comments is empty when no comments`() {
      val credit = createCredit()
      val dto = CreditDto.from(credit)
      assertThat(dto.comments).isEmpty()
    }
  }
}
