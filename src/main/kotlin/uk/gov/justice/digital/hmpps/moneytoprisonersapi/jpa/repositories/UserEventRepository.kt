package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserEvent
import java.time.OffsetDateTime

/** Repository for [UserEvent]. Default ordering: timestamp DESC, id DESC (UEL-006). */
interface UserEventRepository : JpaRepository<UserEvent, Long> {
  fun findAllByOrderByTimestampDescIdDesc(): List<UserEvent>
  fun findByTimestampBefore(timestamp: OffsetDateTime): List<UserEvent>
}
