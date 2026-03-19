package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("Transaction Model")
class TransactionTest {

  private fun createTransaction(
    amount: Long = 1000,
    category: TransactionCategory = TransactionCategory.CREDIT,
    source: TransactionSource = TransactionSource.BANK_TRANSFER,
    senderSortCode: String? = "112233",
    senderAccountNumber: String? = "12345678",
    senderName: String? = "Alice Sender",
    senderRollNumber: String? = null,
    reference: String? = null,
    receivedAt: LocalDateTime? = null,
    refCode: String? = "REF001",
    incompleteSenderInfo: Boolean = false,
    referenceInSenderField: Boolean = false,
    processorTypeCode: String? = null,
    credit: Credit? = null,
  ): Transaction = Transaction(
    amount = amount,
    category = category,
    source = source,
    senderSortCode = senderSortCode,
    senderAccountNumber = senderAccountNumber,
    senderName = senderName,
    senderRollNumber = senderRollNumber,
    reference = reference,
    receivedAt = receivedAt,
    refCode = refCode,
    incompleteSenderInfo = incompleteSenderInfo,
    referenceInSenderField = referenceInSenderField,
    processorTypeCode = processorTypeCode,
    credit = credit,
  )

  @Nested
  @DisplayName("TXN-001: Transaction stores amount, category, source")
  inner class CoreFields {

    @Test
    fun `amount stores value as long`() {
      val txn = createTransaction(amount = 50000)
      assertEquals(50000L, txn.amount)
    }

    @Test
    fun `category stores CREDIT`() {
      val txn = createTransaction(category = TransactionCategory.CREDIT)
      assertEquals(TransactionCategory.CREDIT, txn.category)
    }

    @Test
    fun `category stores DEBIT`() {
      val txn = createTransaction(category = TransactionCategory.DEBIT)
      assertEquals(TransactionCategory.DEBIT, txn.category)
    }

    @Test
    fun `source stores BANK_TRANSFER`() {
      val txn = createTransaction(source = TransactionSource.BANK_TRANSFER)
      assertEquals(TransactionSource.BANK_TRANSFER, txn.source)
    }

    @Test
    fun `source stores ADMINISTRATIVE`() {
      val txn = createTransaction(source = TransactionSource.ADMINISTRATIVE)
      assertEquals(TransactionSource.ADMINISTRATIVE, txn.source)
    }
  }

  @Nested
  @DisplayName("TXN-002: Sender info fields")
  inner class SenderInfo {

    @Test
    fun `sender_sort_code is stored`() {
      val txn = createTransaction(senderSortCode = "445566")
      assertEquals("445566", txn.senderSortCode)
    }

    @Test
    fun `sender_account_number is stored`() {
      val txn = createTransaction(senderAccountNumber = "87654321")
      assertEquals("87654321", txn.senderAccountNumber)
    }

    @Test
    fun `sender_name is stored`() {
      val txn = createTransaction(senderName = "Bob Sender")
      assertEquals("Bob Sender", txn.senderName)
    }

    @Test
    fun `sender_roll_number is stored and nullable`() {
      val txn = createTransaction(senderRollNumber = "ROLL123")
      assertEquals("ROLL123", txn.senderRollNumber)

      val txnNull = createTransaction(senderRollNumber = null)
      assertNull(txnNull.senderRollNumber)
    }

    @Test
    fun `sender fields can all be null`() {
      val txn = createTransaction(
        senderSortCode = null,
        senderAccountNumber = null,
        senderName = null,
        senderRollNumber = null,
      )
      assertNull(txn.senderSortCode)
      assertNull(txn.senderAccountNumber)
      assertNull(txn.senderName)
      assertNull(txn.senderRollNumber)
    }
  }

  @Nested
  @DisplayName("TXN-003: ref_code for reconciliation")
  inner class RefCode {

    @Test
    fun `ref_code stores value`() {
      val txn = createTransaction(refCode = "ABC123")
      assertEquals("ABC123", txn.refCode)
    }

    @Test
    fun `ref_code is nullable`() {
      val txn = createTransaction(refCode = null)
      assertNull(txn.refCode)
    }
  }

  @Nested
  @DisplayName("TXN-004: OneToOne link to Credit is nullable")
  inner class CreditLink {

    @Test
    fun `credit can be null`() {
      val txn = createTransaction(credit = null)
      assertNull(txn.credit)
    }

    @Test
    fun `credit can be set`() {
      val credit = Credit(amount = 5000L)
      val txn = createTransaction(credit = credit)
      assertNotNull(txn.credit)
      assertEquals(5000L, txn.credit!!.amount)
    }
  }

  @Nested
  @DisplayName("TXN-005: incomplete_sender_info flag")
  inner class IncompleteSenderInfo {

    @Test
    fun `incomplete_sender_info defaults to false`() {
      val txn = createTransaction()
      assertFalse(txn.incompleteSenderInfo)
    }

    @Test
    fun `incomplete_sender_info can be set to true`() {
      val txn = createTransaction(incompleteSenderInfo = true)
      assertEquals(true, txn.incompleteSenderInfo)
    }
  }

  @Nested
  @DisplayName("TXN-006: reference_in_sender_field flag")
  inner class ReferenceInSenderField {

    @Test
    fun `reference_in_sender_field defaults to false`() {
      val txn = createTransaction()
      assertFalse(txn.referenceInSenderField)
    }

    @Test
    fun `reference_in_sender_field can be set to true`() {
      val txn = createTransaction(referenceInSenderField = true)
      assertEquals(true, txn.referenceInSenderField)
    }
  }

  @Nested
  @DisplayName("TXN-007: processor_type_code optional")
  inner class ProcessorTypeCode {

    @Test
    fun `processor_type_code is nullable`() {
      val txn = createTransaction(processorTypeCode = null)
      assertNull(txn.processorTypeCode)
    }

    @Test
    fun `processor_type_code stores value when set`() {
      val txn = createTransaction(processorTypeCode = "BACS")
      assertEquals("BACS", txn.processorTypeCode)
    }
  }

  @Nested
  @DisplayName("Timestamps auto-populated")
  inner class Timestamps {

    @Test
    fun `created and modified are null before persistence`() {
      val txn = createTransaction()
      assertNull(txn.created)
      assertNull(txn.modified)
    }

    @Test
    fun `prePersist sets created and modified`() {
      val txn = createTransaction()
      txn.onCreate()
      assertNotNull(txn.created)
      assertNotNull(txn.modified)
    }
  }
}
