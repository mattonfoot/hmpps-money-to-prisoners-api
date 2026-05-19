package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.JobInformation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.JobInformationRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("JobInformationService")
class JobInformationServiceTest {

  @Mock
  private lateinit var jobInformationRepository: JobInformationRepository

  @InjectMocks
  private lateinit var service: JobInformationService

  private fun makeUser() = MtpUser(id = 1L, username = "testuser")

  @Test
  fun `AUTH-070 creates and persists job information with title, prisonEstate, tasks`() {
    val user = makeUser()
    val saved = JobInformation(
      id = 1L,
      user = user,
      name = "Finance Officer",
      prisonEstate = "HMPPS",
      tasks = "Processing payments",
    )
    whenever(jobInformationRepository.save(any())).thenReturn(saved)

    val result = service.createJobInformation(
      user = user,
      title = "Finance Officer",
      prisonEstate = "HMPPS",
      tasks = "Processing payments",
    )

    assertThat(result.title).isEqualTo("Finance Officer")
    assertThat(result.prisonEstate).isEqualTo("HMPPS")
    assertThat(result.tasks).isEqualTo("Processing payments")
  }

  @Test
  fun `AUTH-071 links job information to the provided user`() {
    val user = makeUser()
    whenever(jobInformationRepository.save(any())).thenAnswer { it.arguments[0] }

    service.createJobInformation(
      user = user,
      title = "Officer",
      prisonEstate = "Test",
      tasks = "Tasks",
    )

    val captor = argumentCaptor<JobInformation>()
    verify(jobInformationRepository).save(captor.capture())
    assertThat(captor.firstValue.user?.id).isEqualTo(user.id)
  }

  @Test
  fun `AUTH-072 duplicate create for same user propagates unique constraint failure`() {
    val user = makeUser()
    whenever(jobInformationRepository.save(any())).thenThrow(DataIntegrityViolationException("duplicate key value"))

    org.junit.jupiter.api.assertThrows<DataIntegrityViolationException> {
      service.createJobInformation(
        user = user,
        title = "New Title",
        prisonEstate = "New",
        tasks = "New",
      )
    }

    verify(jobInformationRepository, never()).findByUserId(any())
  }
}
