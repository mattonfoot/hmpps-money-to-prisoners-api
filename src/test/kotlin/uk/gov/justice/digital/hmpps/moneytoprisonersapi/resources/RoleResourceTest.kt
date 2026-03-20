package uk.gov.justice.digital.hmpps.moneytoprisonersapi.resources

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpRole
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpRoleRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("RoleResource")
class RoleResourceTest {

  @Mock
  private lateinit var mtpRoleRepository: MtpRoleRepository

  @InjectMocks
  private lateinit var roleResource: RoleResource

  private fun makeRole(name: String, application: String = "cashbook") = MtpRole(id = 1L, name = name, keyGroup = "${name}_group", application = application)

  @Nested
  @DisplayName("GET /roles/ (AUTH-020)")
  inner class ListRoles {

    @Test
    fun `AUTH-020 returns all roles for any authenticated user`() {
      val roles = listOf(
        makeRole("PRISON_CLERK"),
        makeRole("SECURITY_STAFF"),
        makeRole("FIU_OFFICER"),
      )
      whenever(mtpRoleRepository.findAll()).thenReturn(roles)

      val response = roleResource.listRoles()

      assertThat(response.count).isEqualTo(3)
      assertThat(response.results).hasSize(3)
    }

    @Test
    fun `AUTH-020 returns empty list when no roles`() {
      whenever(mtpRoleRepository.findAll()).thenReturn(emptyList())

      val response = roleResource.listRoles()

      assertThat(response.count).isEqualTo(0)
      assertThat(response.results).isEmpty()
    }

    @Test
    fun `AUTH-022 each role has name, keyGroup, otherGroups, application`() {
      val role = MtpRole(
        id = 1L,
        name = "PRISON_CLERK",
        keyGroup = "PrisonClerk",
        otherGroups = "Viewer,Commenter",
        application = "cashbook",
      )
      whenever(mtpRoleRepository.findAll()).thenReturn(listOf(role))

      val response = roleResource.listRoles()

      val dto = response.results[0]
      assertThat(dto.name).isEqualTo("PRISON_CLERK")
      assertThat(dto.keyGroup).isEqualTo("PrisonClerk")
      assertThat(dto.otherGroups).isEqualTo("Viewer,Commenter")
      assertThat(dto.application).isEqualTo("cashbook")
    }

    @Test
    fun `count matches results size`() {
      val roles = listOf(makeRole("ROLE_A"), makeRole("ROLE_B"))
      whenever(mtpRoleRepository.findAll()).thenReturn(roles)

      val response = roleResource.listRoles()

      assertThat(response.count).isEqualTo(response.results.size)
    }
  }
}
