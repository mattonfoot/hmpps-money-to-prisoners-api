package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserEvent

/** Repository for [UserEvent]. Default ordering: timestamp DESC, id DESC (UEL-006). */
interface UserEventRepository : JpaRepository<UserEvent, Long> {
  fun findAllByOrderByTimestampDescIdDesc(): List<UserEvent>
}
