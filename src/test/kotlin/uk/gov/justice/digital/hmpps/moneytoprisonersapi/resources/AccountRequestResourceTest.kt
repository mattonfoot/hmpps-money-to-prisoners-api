package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.AccountRequestDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateAccountRequestRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequestStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.AccountRequestService
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AccountRequestResource")
class AccountRequestResourceTest {

  @Mock
  private lateinit var accountRequestService: AccountRequestService

  @InjectMocks
  private lateinit var resource: AccountRequestResource

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
    status = status,
    created = LocalDateTime.now(),
    modified = LocalDateTime.now(),
  )

  @Nested
  @DisplayName("GET /requests/ (AUTH-061)")
  inner class ListRequests {

    @Test
    fun `AUTH-061 returns paginated list of pending requests`() {
      val requests = listOf(makeRequest(id = 1L), makeRequest(id = 2L))
      whenever(accountRequestService.listPendingRequests(null)).thenReturn(requests)

      val response = resource.listRequests(ordering = null)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body?.count).isEqualTo(2)
    }

    @Test
    fun `AUTH-067 passes ordering parameter to service`() {
      val requests = listOf(makeRequest(id = 2L), makeRequest(id = 1L))
      whenever(accountRequestService.listPendingRequests("-created")).thenReturn(requests)

      val response = resource.listRequests(ordering = "-created")

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body?.count).isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("POST /requests/ (AUTH-060)")
  inner class CreateRequest {

    @Test
    fun `AUTH-060 returns 201 with created request`() {
      val request = makeRequest()
      whenever(accountRequestService.createRequest(any(), any(), any(), any(), any(), any()))
        .thenReturn(request to null)

      val body = CreateAccountRequestRequest(
        username = "newuser",
        firstName = "New",
        lastName = "User",
        email = "new@example.com",
        role = "PRISON_CLERK",
        prison = "LEI",
      )

      val response = resource.createRequest(body)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      val dto = response.body as? AccountRequestDto
      assertThat(dto?.username).isEqualTo("newuser")
    }

    @Test
    fun `returns 400 when username is missing`() {
      val body = CreateAccountRequestRequest(
        username = null,
        firstName = "New",
        lastName = "User",
        email = "new@example.com",
        role = null,
        prison = null,
      )

      val response = resource.createRequest(body)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `AUTH-062 includes existing user info in response when username exists`() {
      val request = makeRequest()
      val existingUser = uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UserDto(
        id = 5L,
        username = "newuser",
        email = "existing@example.com",
        firstName = "Existing",
        lastName = "User",
        isActive = true,
        roleName = "CASHBOOK",
        roleApplication = "cashbook",
        prisonIds = listOf("LEI"),
        isLocked = false,
      )
      whenever(accountRequestService.createRequest(any(), any(), any(), any(), any(), any()))
        .thenReturn(request to existingUser)

      val body = CreateAccountRequestRequest(
        username = "newuser",
        firstName = "New",
        lastName = "User",
        email = "new@example.com",
        role = "PRISON_CLERK",
        prison = "LEI",
      )

      val response = resource.createRequest(body)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      val dto = response.body as? AccountRequestDto
      assertThat(dto?.existingUser).isNotNull
      assertThat(dto?.existingUser?.username).isEqualTo("newuser")
    }
  }

  @Nested
  @DisplayName("PATCH /requests/{id}/ (AUTH-063)")
  inner class AcceptRequest {

    @Test
    fun `AUTH-063 returns 200 with accepted request`() {
      val accepted = makeRequest(status = AccountRequestStatus.ACCEPTED.value)
      whenever(accountRequestService.acceptRequest(1L)).thenReturn(accepted)

      val response = resource.acceptRequest(1L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      val dto = response.body as? AccountRequestDto
      assertThat(dto?.status).isEqualTo(AccountRequestStatus.ACCEPTED.value)
    }

    @Test
    fun `returns 404 when request not found`() {
      whenever(accountRequestService.acceptRequest(99L)).thenReturn(null)

      val response = resource.acceptRequest(99L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
  }

  @Nested
  @DisplayName("DELETE /requests/{id}/ (AUTH-066)")
  inner class RejectRequest {

    @Test
    fun `AUTH-066 returns 204 after rejecting request`() {
      val rejected = makeRequest(status = AccountRequestStatus.REJECTED.value)
      whenever(accountRequestService.rejectRequest(1L)).thenReturn(rejected)

      val response = resource.rejectRequest(1L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `returns 404 when request not found`() {
      whenever(accountRequestService.rejectRequest(99L)).thenReturn(null)

      val response = resource.rejectRequest(99L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
  }
}
