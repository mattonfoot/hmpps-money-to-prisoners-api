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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreateJobInformationRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.JobInformationDto
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.JobInformation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.JobInformationService
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.UserService

@ExtendWith(MockitoExtension::class)
@DisplayName("JobInformationResource")
class JobInformationResourceTest {

  @Mock
  private lateinit var jobInformationService: JobInformationService

  @Mock
  private lateinit var userService: UserService

  @InjectMocks
  private lateinit var resource: JobInformationResource

  private fun makeUser(username: String = "testuser") = MtpUser(id = 1L, username = username)

  private fun makeJobInfo(user: MtpUser) = JobInformation(
    id = 1L,
    user = user,
    title = "Finance Officer",
    prisonEstate = "HMPPS",
    tasks = "Processing payments",
  )

  private fun makePrincipal(username: String = "testuser") = UsernamePasswordAuthenticationToken(username, null, emptyList())

  @Nested
  @DisplayName("POST /job_information/ (AUTH-070)")
  inner class CreateJobInformation {

    @Test
    fun `AUTH-070 returns 201 with created job information`() {
      val user = makeUser()
      val info = makeJobInfo(user)
      whenever(userService.findByUsername("testuser")).thenReturn(user)
      whenever(jobInformationService.createJobInformation(any(), any(), any(), any())).thenReturn(info)

      val body = CreateJobInformationRequest(
        title = "Finance Officer",
        prisonEstate = "HMPPS",
        tasks = "Processing payments",
      )

      val response = resource.createJobInformation(body, makePrincipal())

      assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
      val dto = response.body as? JobInformationDto
      assertThat(dto?.title).isEqualTo("Finance Officer")
      assertThat(dto?.user).isEqualTo(1L)
    }

    @Test
    fun `AUTH-072 returns 401 when user not found for principal`() {
      whenever(userService.findByUsername("unknown")).thenReturn(null)

      val body = CreateJobInformationRequest(title = "T", prisonEstate = "P", tasks = "T")
      val response = resource.createJobInformation(body, makePrincipal("unknown"))

      assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `AUTH-071 user field in response reflects authenticated user`() {
      val user = makeUser("testuser")
      val info = makeJobInfo(user)
      whenever(userService.findByUsername("testuser")).thenReturn(user)
      whenever(jobInformationService.createJobInformation(any(), any(), any(), any())).thenReturn(info)

      val body = CreateJobInformationRequest(title = "T", prisonEstate = "P", tasks = "T")
      val response = resource.createJobInformation(body, makePrincipal("testuser"))

      val dto = response.body as? JobInformationDto
      assertThat(dto?.user).isEqualTo(user.id)
    }
  }
}
