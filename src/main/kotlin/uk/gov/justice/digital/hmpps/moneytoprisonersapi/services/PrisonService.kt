package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.CreatePrisonerLocationRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerCreditNoticeEmail
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonerLocation
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonCategoryRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonPopulationRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerBalanceRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerCreditNoticeEmailRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonerLocationRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PrisonService(
  private val prisonRepository: PrisonRepository,
  private val prisonCategoryRepository: PrisonCategoryRepository,
  private val prisonPopulationRepository: PrisonPopulationRepository,
  private val prisonerLocationRepository: PrisonerLocationRepository,
  private val prisonerBalanceRepository: PrisonerBalanceRepository,
  private val prisonerCreditNoticeEmailRepository: PrisonerCreditNoticeEmailRepository,
) {

  fun listPrisons(excludeEmptyPrisons: Boolean = false): List<Prison> = if (excludeEmptyPrisons) {
    prisonRepository.findAllWithActivePrisonerLocations()
  } else {
    prisonRepository.findAll()
  }

  fun listPrisonCategories() = prisonCategoryRepository.findAll()

  fun listPrisonPopulations() = prisonPopulationRepository.findAll()

  @Transactional
  fun createPrisonerLocations(requests: List<CreatePrisonerLocationRequest>, createdBy: String) {
    for (request in requests) {
      val prison = prisonRepository.findById(request.prison).orElseThrow {
        ResponseStatusException(HttpStatus.BAD_REQUEST, "No prison found with code ${request.prison}")
      }
      // Deactivate existing active locations for this prisoner+prison
      prisonerLocationRepository.deactivateExistingForPrisonerAndPrison(request.prisonerNumber, request.prison)
      // Create new active location
      val location = PrisonerLocation(
        prisonerNumber = request.prisonerNumber,
        prison = prison,
        active = true,
        createdBy = createdBy,
        prisonerDob = request.prisonerDob,
      )
      prisonerLocationRepository.save(location)
    }
  }

  fun getActivePrisonerLocation(prisonerNumber: String): PrisonerLocation? {
    val locations = prisonerLocationRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber)
    return locations.firstOrNull()
  }

  fun canUpload(): Boolean {
    val cutoff = LocalDateTime.now().minusMinutes(10)
    return !prisonerLocationRepository.existsByActiveFalseAndModifiedAfter(cutoff)
  }

  @Transactional
  fun deleteOldLocations() {
    prisonerLocationRepository.deactivateAllActive()
  }

  @Transactional
  fun deleteInactiveLocations() {
    prisonerLocationRepository.deleteByActiveFalse()
  }

  fun checkPrisonerValidity(prisonerNumber: String, prisonerDob: LocalDate): List<PrisonerLocation> = prisonerLocationRepository.findByPrisonerNumberAndPrisonerDobAndActiveTrue(prisonerNumber, prisonerDob)

  fun getPrisonerAccountBalance(prisonerNumber: String): Long {
    val balances = prisonerBalanceRepository.findByPrisonerNumber(prisonerNumber)
    return balances.firstOrNull()?.amount ?: 0L
  }

  fun listCreditNoticeEmails(): List<PrisonerCreditNoticeEmail> = prisonerCreditNoticeEmailRepository.findAll()

  @Transactional
  fun createCreditNoticeEmail(prisonNomisId: String, email: String): PrisonerCreditNoticeEmail {
    val prison = prisonRepository.findById(prisonNomisId).orElseThrow {
      ResponseStatusException(HttpStatus.BAD_REQUEST, "No prison found with code $prisonNomisId")
    }
    val noticeEmail = PrisonerCreditNoticeEmail(prison = prison, email = email)
    return prisonerCreditNoticeEmailRepository.save(noticeEmail)
  }

  @Transactional
  fun updateCreditNoticeEmail(prisonNomisId: String, email: String): PrisonerCreditNoticeEmail {
    val noticeEmail = prisonerCreditNoticeEmailRepository.findById(prisonNomisId).orElseThrow {
      ResponseStatusException(HttpStatus.NOT_FOUND, "No credit notice email found for prison $prisonNomisId")
    }
    noticeEmail.email = email
    return prisonerCreditNoticeEmailRepository.save(noticeEmail)
  }
}
