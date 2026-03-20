package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.JobInformation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.JobInformationRepository

@Service
class JobInformationService(
  private val jobInformationRepository: JobInformationRepository,
) {

  /**
   * AUTH-070: Creates job information with title, prison_estate, and tasks.
   * AUTH-071: Automatically linked to [user] — the user field is read-only after creation.
   */
  @Transactional
  fun createJobInformation(
    user: MtpUser,
    title: String,
    prisonEstate: String,
    tasks: String,
  ): JobInformation = jobInformationRepository.save(
    JobInformation(
      user = user,
      title = title,
      prisonEstate = prisonEstate,
      tasks = tasks,
    ),
  )
}
