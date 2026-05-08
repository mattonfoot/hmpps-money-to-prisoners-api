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
  ): JobInformation {
    // mtp_auth_jobinformation has UNIQUE(user_id) so a re-POST replaces the row.
    val existing = user.id?.let { jobInformationRepository.findByUserId(it) }
    val info = existing ?: JobInformation().apply { this.user = user }
    info.title = title
    info.prisonEstate = prisonEstate
    info.tasks = tasks
    return jobInformationRepository.save(info)
  }
}
