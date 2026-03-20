package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpRoleRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("UserService")
class UserServiceTest {

  @Mock
  private lateinit var mtpUserRepository: MtpUserRepository

  @Mock
  private lateinit var mtpRoleRepository: MtpRoleRepository

  @Mock
  private lateinit var prisonRepository: PrisonRepository

  @Mock
  private lateinit var loginTrackingService: LoginTrackingService

  @InjectMocks
  private lateinit var userService: UserService

  private fun makeRole(name: String = "PRISON_CLERK", application: String = "cashbook") = MtpRole(id = 1L, name = name, keyGroup = "PrisonClerkGroup", application = application)

  private fun makePrison(nomisId: String = "LEI") = Prison(nomisId = nomisId, name = "Leeds Prison")

  private fun makeUser(
    id: Long = 1L,
    username: String = "testuser",
    email: String = "test@example.com",
    role: MtpRole? = null,
  ) = MtpUser(id = id, username = username, email = email, role = role)

  @Nested
  @DisplayName("getUser (AUTH-011)")
  inner class GetUser {

    @Test
    fun `AUTH-011 returns user wrapped with lock status`() {
      val user = makeUser()
      whenever(mtpUserRepository.findById(1L)).thenReturn(Optional.of(user))
      whenever(loginTrackingService.isLocked(user, "")).thenReturn(false)

      val result = userService.getUser(1L)

      assertThat(result).isNotNull
      assertThat(result!!.first).isEqualTo(user)
      assertThat(result.second).isFalse()
    }

    @Test
    fun `returns null when user not found`() {
      whenever(mtpUserRepository.findById(99L)).thenReturn(Optional.empty())

      val result = userService.getUser(99L)

      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("createUser (AUTH-012)")
  inner class CreateUser {

    @Test
    fun `AUTH-012 creates and saves a new user`() {
      whenever(mtpUserRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false)
      whenever(mtpUserRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false)
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      val captor = argumentCaptor<MtpUser>()
      userService.createUser(username = "newuser", email = "new@example.com", firstName = "New", lastName = "User", role = null, prisons = emptySet())
      verify(mtpUserRepository).save(captor.capture())

      assertThat(captor.firstValue.username).isEqualTo("newuser")
      assertThat(captor.firstValue.email).isEqualTo("new@example.com")
      assertThat(captor.firstValue.firstName).isEqualTo("New")
    }

    @Test
    fun `AUTH-015 throws when username already exists (case-insensitive)`() {
      whenever(mtpUserRepository.existsByUsernameIgnoreCase("EXISTING")).thenReturn(true)

      val ex = assertThrows<IllegalArgumentException> {
        userService.createUser(username = "EXISTING", email = "new@example.com", role = null, prisons = emptySet())
      }
      assertThat(ex.message).contains("username")
    }

    @Test
    fun `AUTH-016 throws when email already exists`() {
      whenever(mtpUserRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false)
      whenever(mtpUserRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true)

      val ex = assertThrows<IllegalArgumentException> {
        userService.createUser(username = "newuser", email = "dup@example.com", role = null, prisons = emptySet())
      }
      assertThat(ex.message).contains("email")
    }
  }

  @Nested
  @DisplayName("updateUser (AUTH-013)")
  inner class UpdateUser {

    @Test
    fun `AUTH-013 updates email, firstName, lastName`() {
      val user = makeUser()
      whenever(mtpUserRepository.findById(1L)).thenReturn(Optional.of(user))
      whenever(mtpUserRepository.existsByEmailIgnoreCase("updated@example.com")).thenReturn(false)
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      userService.updateUser(
        id = 1L,
        email = "updated@example.com",
        firstName = "Updated",
        lastName = "Name",
        prisons = null,
        role = null,
        isSelf = false,
      )

      assertThat(user.email).isEqualTo("updated@example.com")
      assertThat(user.firstName).isEqualTo("Updated")
      assertThat(user.lastName).isEqualTo("Name")
    }

    @Test
    fun `AUTH-013 updates prisons when provided`() {
      val user = makeUser()
      val prison = makePrison("LEI")
      whenever(mtpUserRepository.findById(1L)).thenReturn(Optional.of(user))
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      userService.updateUser(
        id = 1L,
        email = null,
        firstName = null,
        lastName = null,
        prisons = setOf(prison),
        role = null,
        isSelf = false,
      )

      assertThat(user.prisons).containsExactly(prison)
    }

    @Test
    fun `AUTH-018 cannot change own role when isSelf is true`() {
      val role = makeRole()
      val user = makeUser(role = role)
      whenever(mtpUserRepository.findById(1L)).thenReturn(Optional.of(user))
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      val newRole = makeRole("SECURITY_STAFF")
      userService.updateUser(
        id = 1L,
        email = null,
        firstName = null,
        lastName = null,
        prisons = null,
        role = newRole,
        isSelf = true,
      )

      // Role unchanged when isSelf=true
      assertThat(user.role).isEqualTo(role)
    }

    @Test
    fun `AUTH-013 can change role when not self`() {
      val oldRole = makeRole("PRISON_CLERK")
      val user = makeUser(role = oldRole)
      val newRole = makeRole("SECURITY_STAFF")
      whenever(mtpUserRepository.findById(1L)).thenReturn(Optional.of(user))
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      userService.updateUser(
        id = 1L,
        email = null,
        firstName = null,
        lastName = null,
        prisons = null,
        role = newRole,
        isSelf = false,
      )

      assertThat(user.role).isEqualTo(newRole)
    }

    @Test
    fun `returns null when user not found`() {
      whenever(mtpUserRepository.findById(99L)).thenReturn(Optional.empty())

      val result = userService.updateUser(99L, null, null, null, null, null, false)

      assertThat(result).isNull()
    }
  }

  @Nested
  @DisplayName("deactivateUser (AUTH-014)")
  inner class DeactivateUser {

    @Test
    fun `AUTH-014 sets isActive to false without deleting`() {
      val user = makeUser()
      whenever(mtpUserRepository.findById(1L)).thenReturn(Optional.of(user))
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      userService.deactivateUser(1L)

      assertThat(user.isActive).isFalse()
      verify(mtpUserRepository).save(user)
    }

    @Test
    fun `returns null when user not found`() {
      whenever(mtpUserRepository.findById(99L)).thenReturn(Optional.empty())

      val result = userService.deactivateUser(99L)

      assertThat(result).isNull()
    }
  }
}
