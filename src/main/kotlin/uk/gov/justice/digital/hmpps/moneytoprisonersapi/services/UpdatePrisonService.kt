package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdatePrisonRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository

@Service
class UpdatePrisonService(
  private val creditRepository: CreditRepository,
  private val prisonRepository: PrisonRepository,
) {
  @Transactional
  fun updatePrisons(requests: List<UpdatePrisonRequest>) {
    if (requests.isEmpty()) return
    for (request in requests) {
      val prison = prisonRepository.findById(request.prison).orElse(null) ?: continue
      val credits = creditRepository.findByPrisonerNumberAndPrisonIsNull(request.prisonerNumber)
      for (credit in credits) {
        credit.prison = prison
        creditRepository.save(credit)
      }
    }
  }
}
