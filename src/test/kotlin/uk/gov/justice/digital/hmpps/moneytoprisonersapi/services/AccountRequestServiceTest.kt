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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequestStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.AccountRequestRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpRoleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import java.time.LocalDateTime
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

  @InjectMocks
  private lateinit var service: AccountRequestService

  private fun makeRole(name: String = "PRISON_CLERK") = MtpRole(id = 1L, name = name, keyGroup = "group", application = "cashbook")

  private fun makePrison(nomisId: String = "LEI") = Prison(nomisId = nomisId, name = "Leeds Prison")

  private fun makeUser(username: String = "testuser") = MtpUser(
    id = 10L,
    username = username,
    email = "test@example.com",
    firstName = "Test",
    lastName = "User",
  )

  private fun makeRequest(
    id: Long = 1L,
    username: String = "newuser",
    status: String = AccountRequestStatus.PENDING.value,
  ) = AccountRequest(
    id = id,
    username = username,
    firstName = "New",
    lastName = "User",
    email = "new@example.com",
    role = makeRole(),
    prison = makePrison(),
    status = status,
    created = LocalDateTime.now(),
    modified = LocalDateTime.now(),
  )

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

      val (request, existingUser) = service.createRequest(
        username = "newuser",
        firstName = "New",
        lastName = "User",
        email = "new@example.com",
        roleName = "PRISON_CLERK",
        prisonId = "LEI",
      )

      assertThat(request.username).isEqualTo("newuser")
      assertThat(existingUser).isNull()
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

      val (_, existingUser) = service.createRequest(
        username = "newuser",
        firstName = "New",
        lastName = "User",
        email = "new@example.com",
        roleName = "PRISON_CLERK",
        prisonId = "LEI",
      )

      assertThat(existingUser).isNotNull
      assertThat(existingUser!!.username).isEqualTo("newuser")
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
      whenever(accountRequestRepository.save(any())).thenAnswer { it.arguments[0] }

      val result = service.acceptRequest(1L)

      assertThat(result).isNotNull
      val captor = argumentCaptor<MtpUser>()
      verify(mtpUserRepository).save(captor.capture())
      assertThat(captor.firstValue.username).isEqualTo("brandnew")

      val reqCaptor = argumentCaptor<AccountRequest>()
      verify(accountRequestRepository).save(reqCaptor.capture())
      assertThat(reqCaptor.firstValue.status).isEqualTo(AccountRequestStatus.ACCEPTED.value)
    }

    @Test
    fun `AUTH-063 updates existing user role when username already exists`() {
      val request = makeRequest(id = 1L, username = "existinguser")
      val existing = makeUser("existinguser")
      val newRole = makeRole("NEW_ROLE")
      request.role = newRole
      whenever(accountRequestRepository.findById(1L)).thenReturn(Optional.of(request))
      whenever(mtpUserRepository.findByUsernameIgnoreCase("existinguser")).thenReturn(existing)
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }
      whenever(accountRequestRepository.save(any())).thenAnswer { it.arguments[0] }

      val result = service.acceptRequest(1L)

      assertThat(result).isNotNull
      val captor = argumentCaptor<MtpUser>()
      verify(mtpUserRepository).save(captor.capture())
      assertThat(captor.firstValue.role?.name).isEqualTo("NEW_ROLE")
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
    fun `AUTH-066 sets status to rejected`() {
      val request = makeRequest(id = 1L)
      whenever(accountRequestRepository.findById(1L)).thenReturn(Optional.of(request))
      whenever(accountRequestRepository.save(any())).thenAnswer { it.arguments[0] }

      val result = service.rejectRequest(1L)

      assertThat(result).isNotNull
      val captor = argumentCaptor<AccountRequest>()
      verify(accountRequestRepository).save(captor.capture())
      assertThat(captor.firstValue.status).isEqualTo(AccountRequestStatus.REJECTED.value)
    }
  }
}
