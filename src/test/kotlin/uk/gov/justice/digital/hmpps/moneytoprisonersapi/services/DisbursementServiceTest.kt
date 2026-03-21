package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementActionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementConfirmItem
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementConfirmRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementLog
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.InvalidDisbursementStateException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementLogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class DisbursementServiceTest {

  private val disbursementRepository: DisbursementRepository = mock()
  private val disbursementLogRepository: DisbursementLogRepository = mock()
  private val prisonerProfileRepository: PrisonerProfileRepository = mock()
  private lateinit var disbursementService: DisbursementService

  @BeforeEach
  fun setUp() {
    disbursementService = DisbursementService(
      disbursementRepository = disbursementRepository,
      disbursementLogRepository = disbursementLogRepository,
      prisonerProfileRepository = prisonerProfileRepository,
    )
  }

  @Nested
  @DisplayName("DSB-030: Create Disbursement")
  inner class CreateDisbursement {

    @Test
    @DisplayName("DSB-030 - Creates disbursement with PENDING resolution and logs CREATED")
    fun `should create disbursement with PENDING resolution and log`() {
      val request = CreateDisbursementRequest(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )

      val savedDisbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(savedDisbursement)

      val result = disbursementService.createDisbursement(request, "clerk1")

      assertThat(result.resolution).isEqualTo(DisbursementResolution.PENDING)
      verify(disbursementLogRepository).save(org.mockito.kotlin.any())
    }
  }

  @Nested
  @DisplayName("DSB-035: Update Disbursement")
  inner class UpdateDisbursement {

    @Test
    @DisplayName("DSB-035 - Can update PENDING disbursement")
    fun `should update pending disbursement`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      `when`(disbursementRepository.findById(1L)).thenReturn(Optional.of(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      val request = UpdateDisbursementRequest(amount = 6000L)
      disbursementService.updateDisbursement(1L, request, "clerk1")

      assertThat(disbursement.amount).isEqualTo(6000L)
    }

    @Test
    @DisplayName("DSB-036 - Cannot update non-PENDING disbursement - throws exception")
    fun `should throw for non-pending disbursement update`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        resolution = DisbursementResolution.PRECONFIRMED,
      )
      `when`(disbursementRepository.findById(1L)).thenReturn(Optional.of(disbursement))

      val request = UpdateDisbursementRequest(amount = 6000L)
      assertThatThrownBy { disbursementService.updateDisbursement(1L, request, "clerk1") }
        .isInstanceOf(DisbursementNotPendingException::class.java)
    }
  }

  @Nested
  @DisplayName("DSB-040 to DSB-049: Bulk Actions")
  inner class BulkActions {

    @Test
    @DisplayName("DSB-040 - Reject transitions disbursements to REJECTED")
    fun `should reject disbursements`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      disbursementService.reject(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.REJECTED)
    }

    @Test
    @DisplayName("DSB-041 - Preconfirm transitions disbursements to PRECONFIRMED")
    fun `should preconfirm disbursements`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      disbursementService.preconfirm(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PRECONFIRMED)
    }

    @Test
    @DisplayName("DSB-042 - Reset transitions disbursements to PENDING")
    fun `should reset disbursements to PENDING`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        resolution = DisbursementResolution.PRECONFIRMED,
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      disbursementService.reset(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PENDING)
    }

    @Test
    @DisplayName("DSB-043 - Confirm transitions disbursements to CONFIRMED with invoice number")
    fun `should confirm disbursements and set invoice number`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        resolution = DisbursementResolution.PRECONFIRMED,
      )
      // Simulate the disbursement has an id
      val disbursementWithId = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        resolution = DisbursementResolution.PRECONFIRMED,
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(42L))).thenReturn(listOf(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursementWithId)

      val request = DisbursementConfirmRequest(
        disbursements = listOf(DisbursementConfirmItem(id = 42L, nomisTransactionId = "TXN-001")),
      )
      disbursementService.confirm(request, "clerk1")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.CONFIRMED)
    }

    @Test
    @DisplayName("DSB-044 - Send transitions disbursements to SENT")
    fun `should send disbursements`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        resolution = DisbursementResolution.CONFIRMED,
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      disbursementService.send(DisbursementActionRequest(disbursementIds = listOf(1L)), "bankadmin")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.SENT)
    }

    @Test
    @DisplayName("DSB-045 - Invalid transition throws InvalidDisbursementStateException (all-or-nothing)")
    fun `should throw on invalid transition in bulk action`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        resolution = DisbursementResolution.SENT, // terminal state
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))

      assertThatThrownBy {
        disbursementService.reject(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")
      }.isInstanceOf(InvalidDisbursementStateException::class.java)
    }
  }

  @Nested
  @DisplayName("DSB-060 to DSB-073: Filtering")
  inner class Filtering {

    @Test
    @DisplayName("DSB-060 - Filter by exact amount")
    fun `should filter by exact amount`() {
      val d1 = disbursement(amount = 5000L)
      val d2 = disbursement(amount = 3000L)
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(amount = 5000L)
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-061 - Filter by amount gte")
    fun `should filter by amount greater than or equal`() {
      val d1 = disbursement(amount = 5000L)
      val d2 = disbursement(amount = 3000L)
      val d3 = disbursement(amount = 1000L)
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2, d3))

      val result = disbursementService.listDisbursements(amountGte = 3000L)
      assertThat(result).containsExactlyInAnyOrder(d1, d2)
    }

    @Test
    @DisplayName("DSB-062 - Filter by amount lte")
    fun `should filter by amount less than or equal`() {
      val d1 = disbursement(amount = 5000L)
      val d2 = disbursement(amount = 3000L)
      val d3 = disbursement(amount = 1000L)
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2, d3))

      val result = disbursementService.listDisbursements(amountLte = 3000L)
      assertThat(result).containsExactlyInAnyOrder(d2, d3)
    }

    @Test
    @DisplayName("DSB-063 - Filter by resolution")
    fun `should filter by resolution`() {
      val d1 = disbursement(resolution = DisbursementResolution.PENDING)
      val d2 = disbursement(resolution = DisbursementResolution.CONFIRMED)
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(resolution = listOf(DisbursementResolution.PENDING))
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-064 - Filter by method")
    fun `should filter by method`() {
      val d1 = disbursement(method = DisbursementMethod.BANK_TRANSFER)
      val d2 = disbursement(method = DisbursementMethod.CHEQUE)
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(method = DisbursementMethod.CHEQUE)
      assertThat(result).containsExactly(d2)
    }

    @Test
    @DisplayName("DSB-065 - Filter by prisoner_number (case-insensitive exact)")
    fun `should filter by prisoner number case insensitive`() {
      val d1 = disbursement(prisonerNumber = "A1234BC")
      val d2 = disbursement(prisonerNumber = "B5678DE")
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(prisonerNumber = "a1234bc")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-066 - Filter by prisoner_name (case-insensitive substring)")
    fun `should filter by prisoner name substring`() {
      val d1 = disbursement(prisonerName = "John Smith")
      val d2 = disbursement(prisonerName = "Jane Jones")
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(prisonerName = "john")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-067 - Filter by recipient_name (case-insensitive substring)")
    fun `should filter by recipient name substring`() {
      val d1 = disbursement(recipientFirstName = "Alice", recipientLastName = "Brown")
      val d2 = disbursement(recipientFirstName = "Bob", recipientLastName = "Smith")
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(recipientName = "alice")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-068 - Filter by prison (multiple values)")
    fun `should filter by multiple prisons`() {
      val d1 = disbursement(prison = "LEI")
      val d2 = disbursement(prison = "MDI")
      val d3 = disbursement(prison = "BXI")
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2, d3))

      val result = disbursementService.listDisbursements(prisons = listOf("LEI", "MDI"))
      assertThat(result).containsExactlyInAnyOrder(d1, d2)
    }

    @Test
    @DisplayName("DSB-069 - Filter by sort_code")
    fun `should filter by sort code`() {
      val d1 = disbursement(sortCode = "112233")
      val d2 = disbursement(sortCode = "445566")
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(sortCode = "112233")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-070 - Filter by postcode (normalized: remove spaces, uppercase)")
    fun `should filter by postcode normalized`() {
      val d1 = disbursement(postcode = "SW1A 1AA")
      val d2 = disbursement(postcode = "EC1A 1BB")
      `when`(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      // Should match even with space removed and lowercase
      val result = disbursementService.listDisbursements(postcode = "sw1a1aa")
      assertThat(result).containsExactly(d1)
    }

    private fun disbursement(
      amount: Long = 1000L,
      method: DisbursementMethod = DisbursementMethod.BANK_TRANSFER,
      prison: String = "LEI",
      prisonerNumber: String = "A1234BC",
      prisonerName: String = "John Smith",
      recipientFirstName: String = "Jane",
      recipientLastName: String? = "Doe",
      resolution: DisbursementResolution = DisbursementResolution.PENDING,
      sortCode: String? = null,
      postcode: String? = null,
    ) = Disbursement(
      amount = amount,
      method = method,
      prison = prison,
      prisonerNumber = prisonerNumber,
      prisonerName = prisonerName,
      recipientFirstName = recipientFirstName,
      recipientLastName = recipientLastName,
      resolution = resolution,
      sortCode = sortCode,
      postcode = postcode,
    )
  }

  @Nested
  @DisplayName("DSB-090 to DSB-096: Logging")
  inner class Logging {

    @Test
    @DisplayName("DSB-090 - Log CREATED on create")
    fun `should log CREATED action on create`() {
      val request = CreateDisbursementRequest(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      val savedDisbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(savedDisbursement)

      disbursementService.createDisbursement(request, "clerk1")

      val logCaptor = ArgumentCaptor.forClass(DisbursementLog::class.java)
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.value.action).isEqualTo(LogAction.CREATED)
      assertThat(logCaptor.value.userId).isEqualTo("clerk1")
    }

    @Test
    @DisplayName("DSB-091 - Log EDITED on update with changes")
    fun `should log EDITED action on update with changes`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      `when`(disbursementRepository.findById(1L)).thenReturn(Optional.of(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      val request = UpdateDisbursementRequest(amount = 6000L)
      disbursementService.updateDisbursement(1L, request, "clerk1")

      val logCaptor = ArgumentCaptor.forClass(DisbursementLog::class.java)
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.value.action).isEqualTo(LogAction.EDITED)
    }

    @Test
    @DisplayName("DSB-092 - Log REJECTED on reject action")
    fun `should log REJECTED action on reject`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      disbursementService.reject(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")

      val logCaptor = ArgumentCaptor.forClass(DisbursementLog::class.java)
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.value.action).isEqualTo(LogAction.REJECTED)
    }

    @Test
    @DisplayName("DSB-093 - Log CONFIRMED on confirm action")
    fun `should log CONFIRMED action on confirm`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        resolution = DisbursementResolution.PRECONFIRMED,
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      val request = DisbursementConfirmRequest(
        disbursements = listOf(DisbursementConfirmItem(id = 1L, nomisTransactionId = null)),
      )
      disbursementService.confirm(request, "clerk1")

      val logCaptor = ArgumentCaptor.forClass(DisbursementLog::class.java)
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.value.action).isEqualTo(LogAction.CONFIRMED)
    }

    @Test
    @DisplayName("DSB-094 - Log SENT on send action")
    fun `should log SENT action on send`() {
      val disbursement = Disbursement(
        amount = 5000L,
        method = DisbursementMethod.BANK_TRANSFER,
        prison = "LEI",
        prisonerNumber = "A1234BC",
        prisonerName = "John Smith",
        recipientFirstName = "Jane",
        recipientLastName = "Doe",
        resolution = DisbursementResolution.CONFIRMED,
      )
      `when`(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      `when`(disbursementRepository.save(org.mockito.kotlin.any())).thenReturn(disbursement)

      disbursementService.send(DisbursementActionRequest(disbursementIds = listOf(1L)), "bankadmin")

      val logCaptor = ArgumentCaptor.forClass(DisbursementLog::class.java)
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.value.action).isEqualTo(LogAction.SENT)
    }
  }
}
