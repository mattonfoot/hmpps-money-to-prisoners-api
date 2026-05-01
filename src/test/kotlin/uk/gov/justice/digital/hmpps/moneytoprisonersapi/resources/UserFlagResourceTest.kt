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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateUserFlagRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserFlag
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.UserFlagRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.UserService

@ExtendWith(MockitoExtension::class)
@DisplayName("UsersResource")
class UserFlagResourceTest {

  @Mock
  private lateinit var userFlagRepository: UserFlagRepository

  @Mock
  private lateinit var userService: UserService

  @InjectMocks
  private lateinit var usersResource: UsersResource

  private fun makeUser(id: Long = 1L) = MtpUser(id = id, username = "testuser")
  private fun makeFlag(user: MtpUser, name: String) = UserFlag(id = 10L, user = user, flagName = name)

  @Nested
  @DisplayName("GET /users/{id}/flags/ (AUTH-031)")
  inner class ListFlags {

    @Test
    fun `AUTH-031 returns flags for user`() {
      val user = makeUser()
      whenever(userService.findByUsername("testuser")).thenReturn(user)
      whenever(userFlagRepository.findByUser(user)).thenReturn(
        listOf(makeFlag(user, "flag_a"), makeFlag(user, "flag_b")),
      )

      val response = usersResource.listFlags("testuser")

      assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(response.body?.count).isEqualTo(2)
      assertThat(response.body?.results).containsExactlyInAnyOrder("flag_a", "flag_b")
    }

    @Test
    fun `returns 404 when user not found`() {
      whenever(userService.findByUsername("nonexistent")).thenReturn(null)

      val response = usersResource.listFlags("nonexistent")

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
  }

  @Nested
  @DisplayName("POST /users/{id}/flags/ (AUTH-030)")
  inner class CreateFlag {

    @Test
    fun `AUTH-030 creates flag for user`() {
      val user = makeUser()
      val flag = makeFlag(user, "new_flag")
      whenever(userService.findByUsername("testuser")).thenReturn(user)
      whenever(userFlagRepository.existsByUserAndFlagName(user, "new_flag")).thenReturn(false)
      whenever(userFlagRepository.save(any())).thenReturn(flag)

      val request = CreateUserFlagRequest(flagName = "new_flag")
      val response = usersResource.createFlag("testuser", request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(response.body).isEqualTo("new_flag")
    }

    @Test
    fun `AUTH-030 flag is unique per user - returns 400 on duplicate`() {
      val user = makeUser()
      whenever(userService.findByUsername("testuser")).thenReturn(user)
      whenever(userFlagRepository.existsByUserAndFlagName(user, "existing_flag")).thenReturn(true)

      val request = CreateUserFlagRequest(flagName = "existing_flag")
      val response = usersResource.createFlag("testuser", request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 400 when flagName is null`() {
      val user = makeUser()
      whenever(userService.findByUsername("testuser")).thenReturn(user)

      val request = CreateUserFlagRequest(flagName = null)
      val response = usersResource.createFlag("testuser", request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `returns 404 when user not found`() {
      whenever(userService.findByUsername("nonexistent")).thenReturn(null)

      val request = CreateUserFlagRequest(flagName = "test")
      val response = usersResource.createFlag("nonexistent", request)

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `saves flag with correct user and name`() {
      val user = makeUser()
      whenever(userService.findByUsername("testuser")).thenReturn(user)
      whenever(userFlagRepository.existsByUserAndFlagName(user, "test_flag")).thenReturn(false)
      whenever(userFlagRepository.save(any())).thenAnswer { it.arguments[0] }

      val captor = argumentCaptor<UserFlag>()
      usersResource.createFlag("testuser", CreateUserFlagRequest(flagName = "test_flag"))
      verify(userFlagRepository).save(captor.capture())

      assertThat(captor.firstValue.user).isEqualTo(user)
      assertThat(captor.firstValue.flagName).isEqualTo("test_flag")
    }
  }

  @Nested
  @DisplayName("DELETE /users/{id}/flags/{flagName}/ (AUTH-032)")
  inner class DeleteFlag {

    @Test
    fun `AUTH-032 deletes flag by name`() {
      val user = makeUser()
      val flag = makeFlag(user, "to_delete")
      whenever(userService.findByUsername("testuser")).thenReturn(user)
      whenever(userFlagRepository.findByUserAndFlagName(user, "to_delete")).thenReturn(flag)

      val response = usersResource.deleteFlag("testuser", "to_delete")

      assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
      verify(userFlagRepository).delete(flag)
    }

    @Test
    fun `returns 404 when user not found`() {
      whenever(userService.findByUsername("nonexistent")).thenReturn(null)

      val response = usersResource.deleteFlag("nonexistent", "test")

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `returns 404 when flag not found`() {
      val user = makeUser()
      whenever(userService.findByUsername("testuser")).thenReturn(user)
      whenever(userFlagRepository.findByUserAndFlagName(user, "missing")).thenReturn(null)

      val response = usersResource.deleteFlag("testuser", "missing")

      assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
  }
}
