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
import java.time.OffsetDateTime

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
    SELECT DISTINCT CAST(e.triggered_at AS DATE) AS triggered_at_date
    FROM notification_event e
    LEFT JOIN auth_user u ON u.id = e.user_id
    WHERE (u.username = :username OR e.user_id IS NULL)
      AND (:rules IS NULL OR e.rule = ANY(STRING_TO_ARRAY(:rules, ',')))
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

  /** NOT-003: Visible to user = own events (user.username matches) + global events (user null). */
  fun visibleToUser(username: String): Specification<Event> = Specification { root, _, cb ->
    val userPath = root.get<Any?>("user")
    cb.or(
      cb.equal(userPath.get<String>("username"), username),
      cb.isNull(userPath),
    )
  }

  /** NOT-004: Filter by one or more rule codes. */
  fun ruleIn(rules: List<String>): Specification<Event> = Specification { root, _, _ ->
    root.get<String>("rule").`in`(rules)
  }

  /** NOT-005: triggered_at >= lower bound (inclusive). */
  fun triggeredAtGte(from: OffsetDateTime): Specification<Event> = Specification { root, _, cb ->
    cb.greaterThanOrEqualTo(root.get("triggeredAt"), from)
  }

  /** NOT-005: triggered_at < upper bound (exclusive). */
  fun triggeredAtLt(to: OffsetDateTime): Specification<Event> = Specification { root, _, cb ->
    cb.lessThan(root.get("triggeredAt"), to)
  }

  /**
   * Eagerly fetches the per-kind subevent rows (creditEvent, disbursementEvent,
   * senderProfileEvent, prisonerProfileEvent) and their nested entities, so the
   * DTO mapper can read them outside the JPA session.
   */
  fun fetchAssociations(): Specification<Event> = Specification { root, query, _ ->
    if (query.resultType != Long::class.java && query.resultType != Long::class.javaPrimitiveType) {
      root.fetch<Any, Any>("creditEvent", JoinType.LEFT)
      root.fetch<Any, Any>("disbursementEvent", JoinType.LEFT)
      root.fetch<Any, Any>("senderProfileEvent", JoinType.LEFT)
      root.fetch<Any, Any>("prisonerProfileEvent", JoinType.LEFT)
      root.fetch<Any, Any>("user", JoinType.LEFT)
    }
    null
  }
}
