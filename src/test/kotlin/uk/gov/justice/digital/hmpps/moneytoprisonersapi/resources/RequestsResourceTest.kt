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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.AccountRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateAccountRequestRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.AccountRequestService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AccountRequest as AccountRequestEntity

@ExtendWith(MockitoExtension::class)
@DisplayName("RequestsResource")
class RequestsResourceTest {

  @Mock
  private lateinit var accountRequestService: AccountRequestService

  @InjectMocks
  private lateinit var resource: RequestsResource

  private fun makeRequest(id: Long = 1L, username: String = "newuser") = AccountRequestEntity().apply {
    this.id = id
    this.username = username
    this.firstName = "New"
    this.lastName = "User"
    this.email = "new@example.com"
  }

  @Nested
  @DisplayName("GET /requests/ (AUTH-061)")
  inner class ListRequests {

    private fun authenticatedToken() = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
      "compat-user",
      null,
      listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
    )

    @Test
    fun `AUTH-061 returns paginated list of pending requests`() {
      val requests = listOf(makeRequest(id = 1L), makeRequest(id = 2L))
      whenever(accountRequestService.listPendingRequests(null)).thenReturn(requests)

      val response = resource.listRequests(ordering = null, authentication = authenticatedToken())

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      @Suppress("UNCHECKED_CAST")
      val body = response.body as uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse<*>
      assertThat(body.count).isEqualTo(2)
    }

    @Test
    fun `AUTH-067 passes ordering parameter to service`() {
      val requests = listOf(makeRequest(id = 2L), makeRequest(id = 1L))
      whenever(accountRequestService.listPendingRequests("-created")).thenReturn(requests)

      val response = resource.listRequests(ordering = "-created", authentication = authenticatedToken())

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      @Suppress("UNCHECKED_CAST")
      val body = response.body as uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.PaginatedResponse<*>
      assertThat(body.count).isEqualTo(2)
    }

    @Test
    fun `AUTH-061 anonymous request returns count-only body`() {
      whenever(accountRequestService.listPendingRequests(null))
        .thenReturn(listOf(makeRequest(id = 1L), makeRequest(id = 2L), makeRequest(id = 3L)))

      val response = resource.listRequests(ordering = null, authentication = null)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      @Suppress("UNCHECKED_CAST")
      val body = response.body as Map<String, Any>
      assertThat(body["count"]).isEqualTo(3)
      assertThat(body).doesNotContainKey("results")
    }
  }

  @Nested
  @DisplayName("POST /requests/ (AUTH-060)")
  inner class CreateRequest {

    @Test
    fun `AUTH-060 returns 201 with created request`() {
      val request = makeRequest()
      whenever(accountRequestService.createRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
          uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreateAccountRequestResult.Created(request, null),
        )

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
      val dto = response.body as? AccountRequest
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
        pk = 5L,
        id = 5L,
        username = "newuser",
        email = "existing@example.com",
        firstName = "Existing",
        lastName = "User",
        isActive = true,
        roleName = "CASHBOOK",
        roleApplication = "cashbook",
        prisons = listOf("LEI"),
        flags = emptyList(),
        userAdmin = false,
        isLocked = false,
      )
      whenever(accountRequestService.createRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
          uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreateAccountRequestResult.Created(request, existingUser),
        )

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
      val dto = response.body as? AccountRequest
      assertThat(dto?.existingUser).isNotNull
      assertThat(dto?.existingUser?.username).isEqualTo("newuser")
    }
  }

  @Nested
  @DisplayName("PATCH /requests/{id}/ (AUTH-063)")
  inner class AcceptRequest {

    @Test
    fun `AUTH-063 returns 200 with accepted request`() {
      val accepted = makeRequest()
      whenever(accountRequestService.acceptRequest(1L)).thenReturn(accepted)

      val response = resource.acceptRequest(1L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body).isInstanceOf(AccountRequest::class.java)
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
      val rejected = makeRequest()
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
