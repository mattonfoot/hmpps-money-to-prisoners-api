package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdatePrisonRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository

@Service
class UpdatePrisonService(
  private val creditRepository: CreditRepository,
) {
  @Transactional
  fun updatePrisons(requests: List<UpdatePrisonRequest>) {
    if (requests.isEmpty()) return
    for (request in requests) {
      val credits = creditRepository.findByPrisonerNumberAndPrisonIsNull(request.prisonerNumber)
      for (credit in credits) {
        credit.prison = request.prison
        creditRepository.save(credit)
      }
    }
  }
}
