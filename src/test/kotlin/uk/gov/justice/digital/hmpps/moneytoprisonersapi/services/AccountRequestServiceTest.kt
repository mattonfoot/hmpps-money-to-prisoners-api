package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AccountRequestRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpRoleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonUserMappingRepository
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AccountRequestService")
class AccountRequestServiceTest {

  @Mock
  private lateinit var accountRequestRepository: AccountRequestRepository

  @Mock
  private lateinit var mtpUserRepository: MtpUserRepository

  @Mock
  private lateinit var mtpRoleRepository: MtpRoleRepository

  @Mock
  private lateinit var prisonRepository: PrisonRepository

  @Mock
  private lateinit var prisonUserMappingRepository: PrisonUserMappingRepository

  @InjectMocks
  private lateinit var service: AccountRequestService

  private fun makeRole(name: String = "PRISON_CLERK") = MtpRole().apply {
    this.id = 1L
    this.name = name
  }

  private fun makePrison(nomisId: String = "LEI") = Prison().apply {
    this.nomisId = nomisId
    this.name = "Leeds Prison"
  }

  private fun makeUser(username: String = "testuser") = MtpUser().apply {
    this.id = 10L
    this.username = username
    this.email = "test@example.com"
    this.firstName = "Test"
    this.lastName = "User"
  }

  private fun makeRequest(id: Long = 1L, username: String = "newuser") = AccountRequest().apply {
    this.id = id
    this.username = username
    this.firstName = "New"
    this.lastName = "User"
    this.email = "new@example.com"
    this.reason = "Need access"
    this.managerEmail = "manager@example.com"
    this.role = makeRole()
    this.prison = makePrison()
  }

  @Nested
  @DisplayName("listPendingRequests")
  inner class ListPendingRequests {

    @Test
    fun `AUTH-061 returns all pending requests ordered by created asc by default`() {
      val requests = listOf(makeRequest(id = 1L), makeRequest(id = 2L))
      whenever(accountRequestRepository.findAllPendingOrderByCreatedAsc()).thenReturn(requests)

      val result = service.listPendingRequests(ordering = null)

      assertThat(result).hasSize(2)
      verify(accountRequestRepository).findAllPendingOrderByCreatedAsc()
    }

    @Test
    fun `AUTH-067 orders by created desc when ordering=-created`() {
      val requests = listOf(makeRequest(id = 2L), makeRequest(id = 1L))
      whenever(accountRequestRepository.findAllPendingOrderByCreatedDesc()).thenReturn(requests)

      val result = service.listPendingRequests(ordering = "-created")

      assertThat(result).hasSize(2)
      verify(accountRequestRepository).findAllPendingOrderByCreatedDesc()
    }
  }

  @Nested
  @DisplayName("createRequest")
  inner class CreateRequest {

    @Test
    fun `AUTH-060 creates and persists an account request`() {
      val role = makeRole()
      val prison = makePrison()
      whenever(mtpRoleRepository.findByName("PRISON_CLERK")).thenReturn(role)
      whenever(prisonRepository.findById("LEI")).thenReturn(Optional.of(prison))
      whenever(mtpUserRepository.findByUsernameIgnoreCase("newuser")).thenReturn(null)
      val saved = makeRequest()
      whenever(accountRequestRepository.save(any())).thenReturn(saved)

      val result = service.createRequest(
        username = "newuser",
        firstName = "New",
        lastName = "User",
        email = "new@example.com",
        reason = "Need access",
        managerEmail = "manager@example.com",
        roleName = "PRISON_CLERK",
        prisonId = "LEI",
      )

      assertThat(result).isInstanceOf(CreateAccountRequestResult.Created::class.java)
      val created = result as CreateAccountRequestResult.Created
      assertThat(created.request.username).isEqualTo("newuser")
      assertThat(created.request.reason).isEqualTo("Need access")
      assertThat(created.request.managerEmail).isEqualTo("manager@example.com")
      assertThat(created.existingUser).isNull()
    }

    @Test
    fun `AUTH-062 returns existing user info when username already exists`() {
      val role = makeRole()
      val prison = makePrison()
      val existing = makeUser("newuser")
      whenever(mtpRoleRepository.findByName("PRISON_CLERK")).thenReturn(role)
      whenever(prisonRepository.findById("LEI")).thenReturn(Optional.of(prison))
      whenever(mtpUserRepository.findByUsernameIgnoreCase("newuser")).thenReturn(existing)
      val saved = makeRequest()
      whenever(accountRequestRepository.save(any())).thenReturn(saved)

      val result = service.createRequest(
        username = "newuser",
        firstName = "New",
        lastName = "User",
        email = "new@example.com",
        reason = "Need access",
        managerEmail = "manager@example.com",
        roleName = "PRISON_CLERK",
        prisonId = "LEI",
      )

      assertThat(result).isInstanceOf(CreateAccountRequestResult.Created::class.java)
      val created = result as CreateAccountRequestResult.Created
      assertThat(created.existingUser).isNotNull
      assertThat(created.existingUser!!.username).isEqualTo("newuser")
    }

    @Test
    fun `persists reason and manager email`() {
      val role = makeRole(name = "security")
      val existing = makeRequest().apply {
        this.role = role
        this.prison = null
        this.reason = "Need access"
        this.managerEmail = "manager@example.com"
      }
      whenever(mtpRoleRepository.findByName("security")).thenReturn(role)
      whenever(accountRequestRepository.save(any())).thenReturn(existing)

      service.createRequest(
        username = "newuser",
        firstName = "New",
        lastName = "User",
        email = "new@example.com",
        reason = "Need access",
        managerEmail = "manager@example.com",
        roleName = "security",
        prisonId = null,
      )

      val captor = argumentCaptor<AccountRequest>()
      verify(accountRequestRepository).save(captor.capture())
      assertThat(captor.firstValue.reason).isEqualTo("Need access")
      assertThat(captor.firstValue.managerEmail).isEqualTo("manager@example.com")
    }
  }

  @Nested
  @DisplayName("acceptRequest")
  inner class AcceptRequest {

    @Test
    fun `AUTH-063 returns null when request not found`() {
      whenever(accountRequestRepository.findById(99L)).thenReturn(Optional.empty())
      assertThat(service.acceptRequest(99L)).isNull()
    }

    @Test
    fun `AUTH-063 creates new user when no existing user with that username`() {
      val request = makeRequest(id = 1L, username = "brandnew")
      whenever(accountRequestRepository.findById(1L)).thenReturn(Optional.of(request))
      whenever(mtpUserRepository.findByUsernameIgnoreCase("brandnew")).thenReturn(null)
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      val result = service.acceptRequest(1L)

      assertThat(result).isNotNull
      val captor = argumentCaptor<MtpUser>()
      // First save creates the user; second adds the role's key group via user.groups.
      verify(mtpUserRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture())
      assertThat(captor.firstValue.username).isEqualTo("brandnew")
      verify(accountRequestRepository).delete(request)
    }

    @Test
    fun `AUTH-063 updates existing user when username already exists`() {
      val request = makeRequest(id = 1L, username = "existinguser")
      val existing = makeUser("existinguser")
      whenever(accountRequestRepository.findById(1L)).thenReturn(Optional.of(request))
      whenever(mtpUserRepository.findByUsernameIgnoreCase("existinguser")).thenReturn(existing)
      whenever(mtpUserRepository.save(existing)).thenReturn(existing)

      val result = service.acceptRequest(1L)

      assertThat(result).isNotNull
      verify(mtpUserRepository, org.mockito.Mockito.atLeastOnce()).save(existing)
      verify(accountRequestRepository).delete(request)
    }
  }

  @Nested
  @DisplayName("rejectRequest")
  inner class RejectRequest {

    @Test
    fun `AUTH-066 returns null when request not found`() {
      whenever(accountRequestRepository.findById(99L)).thenReturn(Optional.empty())
      assertThat(service.rejectRequest(99L)).isNull()
    }

    @Test
    fun `AUTH-066 deletes the request row on reject`() {
      val request = makeRequest(id = 1L)
      whenever(accountRequestRepository.findById(1L)).thenReturn(Optional.of(request))

      val result = service.rejectRequest(1L)

      assertThat(result).isNotNull
      verify(accountRequestRepository).delete(request)
    }
  }
}
