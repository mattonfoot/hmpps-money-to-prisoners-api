package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Event
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface EventRepository :
  JpaRepository<Event, Long>,
  JpaSpecificationExecutor<Event> {

  /**
   * NOT-007: Returns distinct local dates of events visible to the user,
   * ordered newest-first, for date-based pagination.
   */
  @Query(
    value = """
    SELECT DISTINCT CAST(triggered_at AS DATE) AS triggered_at_date
    FROM notification_events
    WHERE (username = :username OR username IS NULL)
      AND (:rules IS NULL OR rule = ANY(STRING_TO_ARRAY(:rules, ',')))
    ORDER BY triggered_at_date DESC
    """,
    nativeQuery = true,
  )
  fun findDistinctDatesPaged(
    @Param("username") username: String,
    @Param("rules") rules: String?,
  ): List<LocalDate>
}

object EventSpecifications {

  /** NOT-003: Visible to user = own events (username matches) + global events (username null). */
  fun visibleToUser(username: String): Specification<Event> = Specification { root, _, cb ->
    cb.or(
      cb.equal(root.get<String>("username"), username),
      cb.isNull(root.get<String>("username")),
    )
  }

  /** NOT-004: Filter by one or more rule codes. */
  fun ruleIn(rules: List<String>): Specification<Event> = Specification { root, _, _ ->
    root.get<String>("rule").`in`(rules)
  }

  /** NOT-005: triggered_at >= lower bound (inclusive). */
  fun triggeredAtGte(from: LocalDateTime): Specification<Event> = Specification { root, _, cb ->
    cb.greaterThanOrEqualTo(root.get("triggeredAt"), from)
  }

  /** NOT-005: triggered_at < upper bound (exclusive). */
  fun triggeredAtLt(to: LocalDateTime): Specification<Event> = Specification { root, _, cb ->
    cb.lessThan(root.get("triggeredAt"), to)
  }

  /**
   * Eagerly fetches credit, disbursement, senderProfile, and prisonerProfile
   * to avoid LazyInitializationException when mapping to EventDto.
   */
  fun fetchAssociations(): Specification<Event> = Specification { root, query, _ ->
    // Skip fetch joins on count queries (JPA Specifications run on both data and count queries)
    if (query?.resultType != Long::class.java && query?.resultType != Long::class.javaPrimitiveType) {
      root.fetch<Any, Any>("credit", JoinType.LEFT)
      root.fetch<Any, Any>("disbursement", JoinType.LEFT)
      root.fetch<Any, Any>("senderProfile", JoinType.LEFT)
      root.fetch<Any, Any>("prisonerProfile", JoinType.LEFT)
    }
    null
  }
}
