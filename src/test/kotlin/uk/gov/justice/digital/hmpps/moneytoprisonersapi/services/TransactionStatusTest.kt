package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Transaction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionCategory
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.TransactionSource

@DisplayName("Transaction Status Computation")
class TransactionStatusTest {

  private fun makeCredit(
    prison: String? = null,
    blocked: Boolean = false,
  ) = Credit(
    amount = 1000L,
    prison = prison,
    blocked = blocked,
  )

  private fun makeTxn(
    category: TransactionCategory = TransactionCategory.CREDIT,
    source: TransactionSource = TransactionSource.BANK_TRANSFER,
    incompleteSenderInfo: Boolean = false,
    credit: Credit? = null,
  ) = Transaction(
    amount = 1000L,
    category = category,
    source = source,
    incompleteSenderInfo = incompleteSenderInfo,
    credit = credit,
  )

  @Nested
  @DisplayName("TXN-010: Creditable — credit exists, prison assigned, not blocked")
  inner class Creditable {

    @Test
    fun `transaction with credit, prison assigned and not blocked is creditable`() {
      val credit = makeCredit(prison = "LEI", blocked = false)
      val txn = makeTxn(credit = credit)
      assertEquals(TransactionStatus.CREDITABLE, TransactionStatus.computeFrom(txn))
    }

    @Test
    fun `transaction with credit but no prison is not creditable`() {
      val credit = makeCredit(prison = null, blocked = false)
      val txn = makeTxn(credit = credit)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.CREDITABLE)
    }

    @Test
    fun `transaction with credit, prison but blocked is not creditable`() {
      val credit = makeCredit(prison = "LEI", blocked = true)
      val txn = makeTxn(credit = credit)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.CREDITABLE)
    }
  }

  @Nested
  @DisplayName("TXN-011: Refundable — credit exists, sender info complete, no prison or blocked")
  inner class Refundable {

    @Test
    fun `transaction with credit, complete sender info, no prison is refundable`() {
      val credit = makeCredit(prison = null, blocked = false)
      val txn = makeTxn(credit = credit, incompleteSenderInfo = false)
      assertEquals(TransactionStatus.REFUNDABLE, TransactionStatus.computeFrom(txn))
    }

    @Test
    fun `transaction with credit, complete sender info and blocked is refundable`() {
      val credit = makeCredit(prison = "LEI", blocked = true)
      val txn = makeTxn(credit = credit, incompleteSenderInfo = false)
      assertEquals(TransactionStatus.REFUNDABLE, TransactionStatus.computeFrom(txn))
    }

    @Test
    fun `transaction with credit, incomplete sender info and no prison is not refundable`() {
      val credit = makeCredit(prison = null, blocked = false)
      val txn = makeTxn(credit = credit, incompleteSenderInfo = true)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.REFUNDABLE)
    }
  }

  @Nested
  @DisplayName("TXN-012: Unidentified — incomplete sender, no prison, bank_transfer, credit exists")
  inner class Unidentified {

    @Test
    fun `incomplete sender, bank_transfer credit with no prison is unidentified`() {
      val credit = makeCredit(prison = null)
      val txn = makeTxn(source = TransactionSource.BANK_TRANSFER, incompleteSenderInfo = true, credit = credit)
      assertEquals(TransactionStatus.UNIDENTIFIED, TransactionStatus.computeFrom(txn))
    }

    @Test
    fun `incomplete sender, bank_transfer credit with prison assigned is not unidentified`() {
      val credit = makeCredit(prison = "LEI")
      val txn = makeTxn(source = TransactionSource.BANK_TRANSFER, incompleteSenderInfo = true, credit = credit)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.UNIDENTIFIED)
    }

    @Test
    fun `incomplete sender, administrative source with no prison is not unidentified`() {
      val credit = makeCredit(prison = null)
      val txn = makeTxn(source = TransactionSource.ADMINISTRATIVE, incompleteSenderInfo = true, credit = credit)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.UNIDENTIFIED)
    }
  }

  @Nested
  @DisplayName("TXN-013: Anonymous — incomplete sender, bank_transfer, no credit")
  inner class Anonymous {

    @Test
    fun `incomplete sender, bank_transfer and no credit is anonymous`() {
      val txn = makeTxn(source = TransactionSource.BANK_TRANSFER, incompleteSenderInfo = true, credit = null)
      assertEquals(TransactionStatus.ANONYMOUS, TransactionStatus.computeFrom(txn))
    }

    @Test
    fun `incomplete sender, bank_transfer with credit is not anonymous`() {
      val credit = makeCredit(prison = null)
      val txn = makeTxn(source = TransactionSource.BANK_TRANSFER, incompleteSenderInfo = true, credit = credit)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.ANONYMOUS)
    }

    @Test
    fun `incomplete sender, administrative and no credit is not anonymous`() {
      val txn = makeTxn(source = TransactionSource.ADMINISTRATIVE, incompleteSenderInfo = true, credit = null)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.ANONYMOUS)
    }
  }

  @Nested
  @DisplayName("TXN-014: Anomalous — credit category + administrative source")
  inner class Anomalous {

    @Test
    fun `credit category with administrative source is anomalous`() {
      val txn = makeTxn(category = TransactionCategory.CREDIT, source = TransactionSource.ADMINISTRATIVE)
      assertEquals(TransactionStatus.ANOMALOUS, TransactionStatus.computeFrom(txn))
    }

    @Test
    fun `debit category with administrative source is not anomalous`() {
      val txn = makeTxn(category = TransactionCategory.DEBIT, source = TransactionSource.ADMINISTRATIVE)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.ANOMALOUS)
    }

    @Test
    fun `credit category with bank_transfer source is not anomalous`() {
      val credit = makeCredit(prison = "LEI", blocked = false)
      val txn = makeTxn(category = TransactionCategory.CREDIT, source = TransactionSource.BANK_TRANSFER, credit = credit)
      val status = TransactionStatus.computeFrom(txn)
      assert(status != TransactionStatus.ANOMALOUS)
    }
  }
}
