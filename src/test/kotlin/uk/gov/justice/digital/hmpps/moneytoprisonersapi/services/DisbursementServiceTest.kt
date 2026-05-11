package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.DisbursementNotPendingException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementActionRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementConfirmItem
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.DisbursementConfirmRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateDisbursementRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AuthUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Disbursement
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementLog
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.InvalidDisbursementStateException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.LogAction
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AuthUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementLogRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.DisbursementRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerProfileRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("DisbursementService")
class DisbursementServiceTest {

  @Mock
  private lateinit var disbursementRepository: DisbursementRepository

  @Mock
  private lateinit var disbursementLogRepository: DisbursementLogRepository

  @Mock
  private lateinit var prisonerProfileRepository: PrisonerProfileRepository

  @Mock
  private lateinit var prisonRepository: PrisonRepository

  @Mock
  private lateinit var userRepository: AuthUserRepository

  @InjectMocks
  private lateinit var disbursementService: DisbursementService

  private fun authUser(username: String = "clerk1") = AuthUser().apply {
    this.id = 1L
    this.username = username
  }

  private fun makeDisbursement(
    amount: Int = 5000,
    method: DisbursementMethod = DisbursementMethod.BANK_TRANSFER,
    prisonNomisId: String? = "LEI",
    prisonerNumber: String = "A1234BC",
    prisonerName: String = "John Smith",
    recipientFirstName: String = "Jane",
    recipientLastName: String = "Doe",
    recipientEmail: String? = null,
    recipientIsCompany: Boolean = false,
    resolution: DisbursementResolution = DisbursementResolution.PENDING,
    sortCode: String? = null,
    postcode: String? = null,
    city: String? = null,
    invoiceNumber: String? = null,
    nomisTransactionId: String? = null,
  ): Disbursement = Disbursement().apply {
    this.amount = amount
    this.method = method.value
    this.prison = prisonNomisId?.let { nomis -> PrisonPrison().apply { this.nomisId = nomis } }
    this.prisonerNumber = prisonerNumber
    this.prisonerName = prisonerName
    this.recipientFirstName = recipientFirstName
    this.recipientLastName = recipientLastName
    this.recipientEmail = recipientEmail
    this.recipientIsCompany = recipientIsCompany
    this.resolution = resolution.value
    this.sortCode = sortCode
    this.postcode = postcode
    this.city = city
    this.invoiceNumber = invoiceNumber
    this.nomisTransactionId = nomisTransactionId
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

      val savedDisbursement = makeDisbursement()
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(savedDisbursement)
      whenever(userRepository.findByUsername("clerk1")).thenReturn(authUser())

      val result = disbursementService.createDisbursement(request, "clerk1")

      assertThat(result.resolution).isEqualTo(DisbursementResolution.PENDING.value)
      verify(disbursementLogRepository).save(any<DisbursementLog>())
    }
  }

  @Nested
  @DisplayName("DSB-035: Update Disbursement")
  inner class UpdateDisbursement {

    @Test
    @DisplayName("DSB-035 - Can update PENDING disbursement")
    fun `should update pending disbursement`() {
      val disbursement = makeDisbursement()
      whenever(disbursementRepository.findById(1L)).thenReturn(Optional.of(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)
      whenever(userRepository.findByUsername("clerk1")).thenReturn(authUser())

      val request = UpdateDisbursementRequest(amount = 6000L)
      disbursementService.updateDisbursement(1L, request, "clerk1")

      assertThat(disbursement.amount).isEqualTo(6000)
    }

    @Test
    @DisplayName("DSB-036 - Cannot update non-PENDING disbursement - throws exception")
    fun `should throw for non-pending disbursement update`() {
      val disbursement = makeDisbursement(resolution = DisbursementResolution.PRECONFIRMED)
      whenever(disbursementRepository.findById(1L)).thenReturn(Optional.of(disbursement))

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
      val disbursement = makeDisbursement()
      whenever(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)

      disbursementService.reject(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.REJECTED.value)
    }

    @Test
    @DisplayName("DSB-041 - Preconfirm transitions disbursements to PRECONFIRMED")
    fun `should preconfirm disbursements`() {
      val disbursement = makeDisbursement()
      whenever(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)

      disbursementService.preconfirm(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PRECONFIRMED.value)
    }

    @Test
    @DisplayName("DSB-042 - Reset transitions disbursements to PENDING")
    fun `should reset disbursements to PENDING`() {
      val disbursement = makeDisbursement(resolution = DisbursementResolution.PRECONFIRMED)
      whenever(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)

      disbursementService.reset(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.PENDING.value)
    }

    @Test
    @DisplayName("DSB-043 - Confirm transitions disbursements to CONFIRMED with invoice number")
    fun `should confirm disbursements and set invoice number`() {
      val disbursement = makeDisbursement(resolution = DisbursementResolution.PRECONFIRMED)
      whenever(disbursementRepository.findByIdInWithLock(listOf(42L))).thenReturn(listOf(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)

      val request = DisbursementConfirmRequest(
        disbursements = listOf(DisbursementConfirmItem(id = 42L, nomisTransactionId = "TXN-001")),
      )
      disbursementService.confirm(request, "clerk1")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.CONFIRMED.value)
    }

    @Test
    @DisplayName("DSB-044 - Send transitions disbursements to SENT")
    fun `should send disbursements`() {
      val disbursement = makeDisbursement(resolution = DisbursementResolution.CONFIRMED)
      whenever(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)

      disbursementService.send(DisbursementActionRequest(disbursementIds = listOf(1L)), "bankadmin")

      assertThat(disbursement.resolution).isEqualTo(DisbursementResolution.SENT.value)
    }

    @Test
    @DisplayName("DSB-045 - Invalid transition throws InvalidDisbursementStateException (all-or-nothing)")
    fun `should throw on invalid transition in bulk action`() {
      val disbursement = makeDisbursement(resolution = DisbursementResolution.SENT) // terminal state
      whenever(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))

      assertThatThrownBy {
        disbursementService.reject(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")
      }.isInstanceOf(InvalidDisbursementStateException::class.java)
    }
  }

  @Nested
  @DisplayName("DSB-060 to DSB-081: Filtering")
  inner class Filtering {

    @Test
    @DisplayName("DSB-060 - Filter by exact amount")
    fun `should filter by exact amount`() {
      val d1 = makeDisbursement(amount = 5000)
      val d2 = makeDisbursement(amount = 3000)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(amount = 5000L)
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-061 - Filter by amount gte")
    fun `should filter by amount greater than or equal`() {
      val d1 = makeDisbursement(amount = 5000)
      val d2 = makeDisbursement(amount = 3000)
      val d3 = makeDisbursement(amount = 1000)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2, d3))

      val result = disbursementService.listDisbursements(amountGte = 3000L)
      assertThat(result).containsExactlyInAnyOrder(d1, d2)
    }

    @Test
    @DisplayName("DSB-062 - Filter by amount lte")
    fun `should filter by amount less than or equal`() {
      val d1 = makeDisbursement(amount = 5000)
      val d2 = makeDisbursement(amount = 3000)
      val d3 = makeDisbursement(amount = 1000)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2, d3))

      val result = disbursementService.listDisbursements(amountLte = 3000L)
      assertThat(result).containsExactlyInAnyOrder(d2, d3)
    }

    @Test
    @DisplayName("DSB-063 - Filter by resolution")
    fun `should filter by resolution`() {
      val d1 = makeDisbursement(resolution = DisbursementResolution.PENDING)
      val d2 = makeDisbursement(resolution = DisbursementResolution.CONFIRMED)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(resolution = listOf(DisbursementResolution.PENDING))
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-064 - Filter by method")
    fun `should filter by method`() {
      val d1 = makeDisbursement(method = DisbursementMethod.BANK_TRANSFER)
      val d2 = makeDisbursement(method = DisbursementMethod.CHEQUE)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(method = DisbursementMethod.CHEQUE)
      assertThat(result).containsExactly(d2)
    }

    @Test
    @DisplayName("DSB-065 - Filter by prisoner_number (case-insensitive exact)")
    fun `should filter by prisoner number case insensitive`() {
      val d1 = makeDisbursement(prisonerNumber = "A1234BC")
      val d2 = makeDisbursement(prisonerNumber = "B5678DE")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(prisonerNumber = "a1234bc")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-066 - Filter by prisoner_name (case-insensitive substring)")
    fun `should filter by prisoner name substring`() {
      val d1 = makeDisbursement(prisonerName = "John Smith")
      val d2 = makeDisbursement(prisonerName = "Jane Jones")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(prisonerName = "john")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-067 - Filter by recipient_name (case-insensitive substring)")
    fun `should filter by recipient name substring`() {
      val d1 = makeDisbursement(recipientFirstName = "Alice", recipientLastName = "Brown")
      val d2 = makeDisbursement(recipientFirstName = "Bob", recipientLastName = "Smith")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(recipientName = "alice")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-068 - Filter by prison (multiple values)")
    fun `should filter by multiple prisons`() {
      val d1 = makeDisbursement(prisonNomisId = "LEI")
      val d2 = makeDisbursement(prisonNomisId = "MDI")
      val d3 = makeDisbursement(prisonNomisId = "BXI")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2, d3))

      val result = disbursementService.listDisbursements(prisons = listOf("LEI", "MDI"))
      assertThat(result).containsExactlyInAnyOrder(d1, d2)
    }

    @Test
    @DisplayName("DSB-069 - Filter by sort_code")
    fun `should filter by sort code`() {
      val d1 = makeDisbursement(sortCode = "112233")
      val d2 = makeDisbursement(sortCode = "445566")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(sortCode = "112233")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-070 - Filter by postcode (normalized: remove spaces, uppercase)")
    fun `should filter by postcode normalized`() {
      val d1 = makeDisbursement(postcode = "SW1A 1AA")
      val d2 = makeDisbursement(postcode = "EC1A 1BB")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      // Should match even with space removed and lowercase
      val result = disbursementService.listDisbursements(postcode = "sw1a1aa")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-071 - Filter by city (case-insensitive substring)")
    fun `should filter by city`() {
      val d1 = makeDisbursement(city = "London")
      val d2 = makeDisbursement(city = "Manchester")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(city = "lond")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-072 - Filter by recipient email (case-insensitive substring)")
    fun `should filter by recipient email`() {
      val d1 = makeDisbursement(recipientEmail = "alice@example.com")
      val d2 = makeDisbursement(recipientEmail = "bob@example.com")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(recipientEmail = "alice")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-073 - Filter by recipient is company")
    fun `should filter by recipient is company`() {
      val d1 = makeDisbursement(recipientIsCompany = true)
      val d2 = makeDisbursement(recipientIsCompany = false)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(recipientIsCompany = true)
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-074 - Filter by invoice number (exact)")
    fun `should filter by invoice number`() {
      val d1 = makeDisbursement(invoiceNumber = "PMD1000001")
      val d2 = makeDisbursement(invoiceNumber = "PMD1000002")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(invoiceNumber = "PMD1000001")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-075 - Filter by NOMIS transaction ID (exact)")
    fun `should filter by nomis transaction id`() {
      val d1 = makeDisbursement(nomisTransactionId = "TXN-001")
      val d2 = makeDisbursement(nomisTransactionId = "TXN-002")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(nomisTransactionId = "TXN-001")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-076 - Filter by created date range (gte/lt)")
    fun `should filter by created date range`() {
      val d1 = makeDisbursement().also {
        it.created = OffsetDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC)
      }
      val d2 = makeDisbursement().also {
        it.created = OffsetDateTime.of(2024, 2, 15, 10, 0, 0, 0, ZoneOffset.UTC)
      }
      val d3 = makeDisbursement().also {
        it.created = OffsetDateTime.of(2024, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC)
      }
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2, d3))

      val result = disbursementService.listDisbursements(
        createdGte = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC),
        createdLt = OffsetDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC),
      )
      assertThat(result).containsExactly(d2)
    }

    @Test
    @DisplayName("DSB-077 - Filter by amount ending with suffix")
    fun `should filter by amount endswith`() {
      val d1 = makeDisbursement(amount = 1050)
      val d2 = makeDisbursement(amount = 2000)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(amountEndswith = "50")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-078 - Filter by amount regex")
    fun `should filter by amount regex`() {
      val d1 = makeDisbursement(amount = 1050)
      val d2 = makeDisbursement(amount = 2000)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(amountRegex = "^1.*")
      assertThat(result).containsExactly(d1)
    }

    @Test
    @DisplayName("DSB-079 - Exclude amounts ending with suffix")
    fun `should exclude amount endswith`() {
      val d1 = makeDisbursement(amount = 1050)
      val d2 = makeDisbursement(amount = 2000)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(excludeAmountEndswith = "50")
      assertThat(result).containsExactly(d2)
    }

    @Test
    @DisplayName("DSB-080 - Exclude amounts matching regex")
    fun `should exclude amount regex`() {
      val d1 = makeDisbursement(amount = 1050)
      val d2 = makeDisbursement(amount = 2000)
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(excludeAmountRegex = "^1.*")
      assertThat(result).containsExactly(d2)
    }

    @Test
    @DisplayName("DSB-081 - Simple search across names")
    fun `should simple search across prisoner and recipient names`() {
      val d1 = makeDisbursement(prisonerName = "John Smith", recipientFirstName = "Alice")
      val d2 = makeDisbursement(prisonerName = "Jane Jones", recipientFirstName = "Bob")
      whenever(disbursementRepository.findAll()).thenReturn(listOf(d1, d2))

      val result = disbursementService.listDisbursements(simpleSearch = "john")
      assertThat(result).containsExactly(d1)
    }
  }

  @Nested
  @DisplayName("DSB-090 to DSB-094: Logging")
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
      val savedDisbursement = makeDisbursement()
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(savedDisbursement)
      whenever(userRepository.findByUsername("clerk1")).thenReturn(authUser("clerk1"))

      disbursementService.createDisbursement(request, "clerk1")

      val logCaptor = argumentCaptor<DisbursementLog>()
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.firstValue.action).isEqualTo(LogAction.CREATED.value)
      assertThat(logCaptor.firstValue.user?.username).isEqualTo("clerk1")
    }

    @Test
    @DisplayName("DSB-091 - Log EDITED on update with changes")
    fun `should log EDITED action on update with changes`() {
      val disbursement = makeDisbursement()
      whenever(disbursementRepository.findById(1L)).thenReturn(Optional.of(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)
      whenever(userRepository.findByUsername("clerk1")).thenReturn(authUser("clerk1"))

      val request = UpdateDisbursementRequest(amount = 6000L)
      disbursementService.updateDisbursement(1L, request, "clerk1")

      val logCaptor = argumentCaptor<DisbursementLog>()
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.firstValue.action).isEqualTo(LogAction.EDITED.value)
    }

    @Test
    @DisplayName("DSB-092 - Log REJECTED on reject action")
    fun `should log REJECTED action on reject`() {
      val disbursement = makeDisbursement()
      whenever(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)
      whenever(userRepository.findByUsername("clerk1")).thenReturn(authUser("clerk1"))

      disbursementService.reject(DisbursementActionRequest(disbursementIds = listOf(1L)), "clerk1")

      val logCaptor = argumentCaptor<DisbursementLog>()
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.firstValue.action).isEqualTo(LogAction.REJECTED.value)
    }

    @Test
    @DisplayName("DSB-093 - Log CONFIRMED on confirm action")
    fun `should log CONFIRMED action on confirm`() {
      val disbursement = makeDisbursement(resolution = DisbursementResolution.PRECONFIRMED)
      whenever(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)
      whenever(userRepository.findByUsername("clerk1")).thenReturn(authUser("clerk1"))

      val request = DisbursementConfirmRequest(
        disbursements = listOf(DisbursementConfirmItem(id = 1L, nomisTransactionId = null)),
      )
      disbursementService.confirm(request, "clerk1")

      val logCaptor = argumentCaptor<DisbursementLog>()
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.firstValue.action).isEqualTo(LogAction.CONFIRMED.value)
    }

    @Test
    @DisplayName("DSB-094 - Log SENT on send action")
    fun `should log SENT action on send`() {
      val disbursement = makeDisbursement(resolution = DisbursementResolution.CONFIRMED)
      whenever(disbursementRepository.findByIdInWithLock(listOf(1L))).thenReturn(listOf(disbursement))
      whenever(disbursementRepository.save(any<Disbursement>())).thenReturn(disbursement)
      whenever(userRepository.findByUsername("bankadmin")).thenReturn(authUser("bankadmin"))

      disbursementService.send(DisbursementActionRequest(disbursementIds = listOf(1L)), "bankadmin")

      val logCaptor = argumentCaptor<DisbursementLog>()
      verify(disbursementLogRepository).save(logCaptor.capture())
      assertThat(logCaptor.firstValue.action).isEqualTo(LogAction.SENT.value)
    }
  }
}
