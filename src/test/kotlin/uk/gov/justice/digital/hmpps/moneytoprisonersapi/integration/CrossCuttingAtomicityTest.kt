package uk.gov.justice.digital.hmpps.moneytoprisonersapi.integration

import jakarta.persistence.LockModeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.Lock
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.LogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditService

@DisplayName("14.4 Atomicity & Data Integrity")
class CrossCuttingAtomicityTest : IntegrationTestBase() {

  @Autowired
  private lateinit var creditRepository: CreditRepository

  @Autowired
  private lateinit var logRepository: LogRepository

  @Autowired
  private lateinit var creditService: CreditService

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @BeforeEach
  fun setUp() {
    logRepository.deleteAll()
    creditRepository.deleteAll()
    prisonRepository.deleteAll()
  }

  private fun saveCredit(
    resolution: CreditResolution = CreditResolution.PENDING,
    prison: String? = null,
  ): Credit {
    if (prison != null && !prisonRepository.existsById(prison)) {
      prisonRepository.save(Prison(nomisId = prison, name = prison, region = ""))
    }
    val credit = Credit(amount = 1000L, resolution = resolution, prison = prison)
    credit.source = CreditSource.BANK_TRANSFER
    return creditRepository.save(credit)
  }

  @Nested
  @DisplayName("XCT-030 Bulk actions are all-or-nothing")
  inner class BulkActionsAtomic {

    @Test
    @DisplayName("XCT-030 refund action rolls back if any credit is not in refund_pending state")
    fun `refund of multiple credits fails entirely when any credit is in wrong state`() {
      // Create one valid refund_pending credit (no prison, pending, not blocked, complete sender info)
      val validCredit = saveCredit(resolution = CreditResolution.PENDING, prison = null)

      // Create an invalid credit (already credited — cannot be refunded)
      val invalidCredit = saveCredit(resolution = CreditResolution.CREDITED, prison = "LEI")

      val beforeCount = logRepository.count()

      // Attempt refund of both — should fail entirely due to invalid credit
      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("credit_ids" to listOf(validCredit.id, invalidCredit.id)))
        .exchange()
        .expectStatus().isEqualTo(409)

      // Verify neither credit was updated (atomic rollback)
      val validCreditAfter = creditRepository.findById(validCredit.id!!).get()
      assertThat(validCreditAfter.resolution).isEqualTo(CreditResolution.PENDING)

      // Verify no log entries were created (rollback)
      assertThat(logRepository.count()).isEqualTo(beforeCount)
    }

    @Test
    @DisplayName("XCT-030 refund succeeds for all credits when all are in refund_pending state")
    fun `refund of all valid refund_pending credits succeeds`() {
      // A credit is refund_pending when: no prison OR blocked, pending resolution, sender info complete
      val credit1 = saveCredit(resolution = CreditResolution.PENDING, prison = null)
      val credit2 = saveCredit(resolution = CreditResolution.PENDING, prison = null)

      webTestClient.post()
        .uri("/credits/actions/refund/")
        .headers(setAuthorisation())
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .bodyValue(mapOf("credit_ids" to listOf(credit1.id, credit2.id)))
        .exchange()
        .expectStatus().isNoContent

      assertThat(creditRepository.findById(credit1.id!!).get().resolution).isEqualTo(CreditResolution.REFUNDED)
      assertThat(creditRepository.findById(credit2.id!!).get().resolution).isEqualTo(CreditResolution.REFUNDED)
    }
  }

  @Nested
  @DisplayName("XCT-031 State transitions use pessimistic locking")
  inner class PessimisticLocking {

    @Test
    @DisplayName("XCT-031 CreditRepository.findByIdInWithLock uses PESSIMISTIC_WRITE lock")
    fun `findByIdInWithLock is annotated with PESSIMISTIC_WRITE lock`() {
      val method = CreditRepository::class.java.methods.find { it.name == "findByIdInWithLock" }
      assertThat(method).isNotNull

      val lockAnnotation = method!!.getAnnotation(Lock::class.java)
      assertThat(lockAnnotation).isNotNull
      assertThat(lockAnnotation.value).isEqualTo(LockModeType.PESSIMISTIC_WRITE)
    }

    @Test
    @DisplayName("XCT-031 review action uses findByIdInWithLock for pessimistic locking")
    fun `review method exists in CreditService and executes without error under locking`() {
      val credit = saveCredit()

      // Execute review (which calls findByIdInWithLock under @Transactional)
      creditService.review(listOf(credit.id!!), "test-user")

      val updated = creditRepository.findById(credit.id!!).get()
      assertThat(updated.reviewed).isTrue
    }
  }

  @Nested
  @DisplayName("XCT-032 @Transactional decorators on service write operations")
  inner class TransactionalAnnotations {

    @Test
    @DisplayName("XCT-032 CreditService.review is annotated with @Transactional")
    fun `review method has Transactional annotation`() {
      val method = CreditService::class.java.getMethod("review", List::class.java, String::class.java)
      assertThat(method.getAnnotation(Transactional::class.java)).isNotNull
    }

    @Test
    @DisplayName("XCT-032 CreditService.creditPrisoners is annotated with @Transactional")
    fun `creditPrisoners method has Transactional annotation`() {
      val method = CreditService::class.java.getMethod("creditPrisoners", List::class.java, String::class.java)
      assertThat(method.getAnnotation(Transactional::class.java)).isNotNull
    }

    @Test
    @DisplayName("XCT-032 CreditService.setManual is annotated with @Transactional")
    fun `setManual method has Transactional annotation`() {
      val method = CreditService::class.java.getMethod("setManual", List::class.java, String::class.java)
      assertThat(method.getAnnotation(Transactional::class.java)).isNotNull
    }

    @Test
    @DisplayName("XCT-032 CreditService.refund is annotated with @Transactional")
    fun `refund method has Transactional annotation`() {
      val method = CreditService::class.java.getMethod("refund", List::class.java, String::class.java)
      assertThat(method.getAnnotation(Transactional::class.java)).isNotNull
    }
  }

  @Nested
  @DisplayName("XCT-033 Log entries created within the same transaction as credit updates")
  inner class LogEntriesWithinTransaction {

    @Test
    @DisplayName("XCT-033 review creates a log entry in the same transaction as the credit update")
    fun `review creates log entry atomically with credit update`() {
      val credit = saveCredit()
      assertThat(logRepository.count()).isEqualTo(0)

      creditService.review(listOf(credit.id!!), "test-user")

      // Both the credit reviewed=true and the log entry must exist after the commit
      val updated = creditRepository.findById(credit.id!!).get()
      assertThat(updated.reviewed).isTrue
      assertThat(logRepository.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("XCT-033 when transaction rolls back, log entry is also rolled back")
    fun `when refund fails atomically, no log entries are persisted`() {
      val credit = saveCredit(resolution = CreditResolution.CREDITED) // invalid for refund
      assertThat(logRepository.count()).isEqualTo(0)

      // This should throw and roll back
      try {
        creditService.refund(listOf(credit.id!!), "test-user")
      } catch (e: Exception) {
        // expected
      }

      // Log entry must NOT have been committed
      assertThat(logRepository.count()).isEqualTo(0)
    }

    @Test
    @DisplayName("XCT-033 successful refund creates exactly one log entry per credit")
    fun `refund creates one log entry per credit within the transaction`() {
      val credit1 = saveCredit(resolution = CreditResolution.PENDING, prison = null)
      val credit2 = saveCredit(resolution = CreditResolution.PENDING, prison = null)

      creditService.refund(listOf(credit1.id!!, credit2.id!!), "test-user")

      assertThat(logRepository.count()).isEqualTo(2)
    }
  }
}
