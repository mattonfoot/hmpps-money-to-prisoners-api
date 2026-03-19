package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Comment

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
  fun findByCreditId(creditId: Long): List<Comment>
}
