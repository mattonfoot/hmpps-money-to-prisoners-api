package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateUserRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdateUserRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.UserService
import java.security.Principal

@ExtendWith(MockitoExtension::class)
@DisplayName("UserResource")
class UserResourceTest {

  @Mock
  private lateinit var userService: UserService

  @InjectMocks
  private lateinit var userResource: UserResource

  private val principal = Principal { "testuser" }

  private fun makeRole(name: String = "PRISON_CLERK") = MtpRole(id = 1L, name = name, keyGroup = "PrisonClerkGroup", application = "cashbook")

  private fun makeUser(
    id: Long = 1L,
    username: String = "testuser",
    email: String = "test@example.com",
    role: MtpRole? = null,
  ) = MtpUser(id = id, username = username, email = email, role = role)

  @Nested
  @DisplayName("GET /users/ (AUTH-010)")
  inner class ListUsers {

    @Test
    fun `AUTH-010 returns paginated list of users`() {
      val user = makeUser()
      whenever(userService.listUsers(null, null)).thenReturn(listOf(user to false))

      val response = userResource.listUsers(null, null)

      assertThat(response.count).isEqualTo(1)
      assertThat(response.results).hasSize(1)
      assertThat(response.results[0].username).isEqualTo("testuser")
    }

    @Test
    fun `returns empty list when no users`() {
      whenever(userService.listUsers(null, null)).thenReturn(emptyList())

      val response = userResource.listUsers(null, null)

      assertThat(response.count).isEqualTo(0)
    }

    @Test
    fun `passes role and prison filters to service`() {
      whenever(userService.listUsers("PRISON_CLERK", "LEI")).thenReturn(emptyList())

      val response = userResource.listUsers("PRISON_CLERK", "LEI")

      assertThat(response.count).isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("GET /users/{id}/ (AUTH-011)")
  inner class GetUser {

    @Test
    fun `AUTH-011 returns user with lock status`() {
      val user = makeUser(role = makeRole())
      whenever(userService.getUser(1L)).thenReturn(user to true)

      val response = userResource.getUser(1L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body?.username).isEqualTo("testuser")
      assertThat(response.body?.isLocked).isTrue()
    }

    @Test
    fun `returns 404 when user not found`() {
      whenever(userService.getUser(99L)).thenReturn(null)

      val response = userResource.getUser(99L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
  }

  @Nested
  @DisplayName("POST /users/ (AUTH-012)")
  inner class CreateUser {

    @Test
    fun `AUTH-012 returns 201 with created user`() {
      val role = makeRole()
      val user = makeUser()
      whenever(userService.findRoleByName("PRISON_CLERK")).thenReturn(role)
      whenever(userService.findPrisonsByIds(listOf("LEI"))).thenReturn(emptySet())
      whenever(
        userService.createUser(
          username = "newuser",
          email = "new@example.com",
          firstName = null,
          lastName = null,
          role = role,
          prisons = emptySet(),
        ),
      ).thenReturn(user)

      val request = CreateUserRequest(
        username = "newuser",
        email = "new@example.com",
        roleName = "PRISON_CLERK",
        prisonIds = listOf("LEI"),
      )
      val response = userResource.createUser(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun `returns 400 when username missing`() {
      val request = CreateUserRequest(username = null, email = "new@example.com")

      val response = userResource.createUser(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when email missing`() {
      val request = CreateUserRequest(username = "newuser", email = null)

      val response = userResource.createUser(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 on duplicate username`() {
      whenever(userService.findRoleByName(null)).thenReturn(null)
      whenever(userService.findPrisonsByIds(emptyList())).thenReturn(emptySet())
      whenever(
        userService.createUser(
          username = "dupe",
          email = "new@example.com",
          firstName = null,
          lastName = null,
          role = null,
          prisons = emptySet(),
        ),
      ).thenThrow(IllegalArgumentException("A user with that username already exists"))

      val request = CreateUserRequest(username = "dupe", email = "new@example.com")
      val response = userResource.createUser(request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }

  @Nested
  @DisplayName("PATCH /users/{id}/ (AUTH-013)")
  inner class UpdateUser {

    @Test
    fun `AUTH-013 returns 200 with updated user`() {
      val user = makeUser(username = "other")
      whenever(userService.findRoleByName(null)).thenReturn(null)
      whenever(userService.findPrisonsByIds(emptyList())).thenReturn(emptySet())
      whenever(userService.findById(1L)).thenReturn(user)
      whenever(
        userService.updateUser(
          id = 1L,
          email = "updated@example.com",
          firstName = null,
          lastName = null,
          prisons = emptySet(),
          role = null,
          isSelf = false,
        ),
      ).thenReturn(user)
      whenever(userService.getUser(1L)).thenReturn(user to false)

      val request = UpdateUserRequest(email = "updated@example.com", prisonIds = emptyList())
      val response = userResource.updateUser(1L, request, principal)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `returns 404 when user not found`() {
      whenever(userService.findRoleByName(null)).thenReturn(null)
      whenever(userService.findPrisonsByIds(emptyList())).thenReturn(emptySet())
      whenever(userService.findById(99L)).thenReturn(null)

      val request = UpdateUserRequest(prisonIds = emptyList())
      val response = userResource.updateUser(99L, request, principal)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `AUTH-018 sets isSelf=true when principal username matches target`() {
      val user = makeUser(username = "testuser")
      whenever(userService.findRoleByName(null)).thenReturn(null)
      whenever(userService.findPrisonsByIds(emptyList())).thenReturn(emptySet())
      whenever(userService.findById(1L)).thenReturn(user)
      whenever(
        userService.updateUser(
          id = 1L,
          email = null,
          firstName = "New",
          lastName = null,
          prisons = emptySet(),
          role = null,
          isSelf = true,
        ),
      ).thenReturn(user)
      whenever(userService.getUser(eq(1L))).thenReturn(user to false)

      val request = UpdateUserRequest(firstName = "New", prisonIds = emptyList())
      val response = userResource.updateUser(1L, request, principal)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
  }

  @Nested
  @DisplayName("DELETE /users/{id}/ (AUTH-014)")
  inner class DeleteUser {

    @Test
    fun `AUTH-014 returns 204 and deactivates user`() {
      val user = makeUser()
      whenever(userService.deactivateUser(1L)).thenReturn(user)

      val response = userResource.deleteUser(1L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `returns 404 when user not found`() {
      whenever(userService.deactivateUser(99L)).thenReturn(null)

      val response = userResource.deleteUser(99L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
  }

  @Nested
  @DisplayName("POST /users/{id}/unlock/ (AUTH-017)")
  inner class UnlockUser {

    @Test
    fun `AUTH-017 returns 200 and unlocks user`() {
      val user = makeUser()
      whenever(userService.unlockUser(1L)).thenReturn(user)
      whenever(userService.getUser(1L)).thenReturn(user to false)

      val response = userResource.unlockUser(1L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `returns 404 when user not found`() {
      whenever(userService.unlockUser(99L)).thenReturn(null)

      val response = userResource.unlockUser(99L)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
  }
}
