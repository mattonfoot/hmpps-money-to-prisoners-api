package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailFrequency
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.EmailNotificationPreferences
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Event
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.EmailNotificationPreferencesRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.EventRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.EventSpecifications
import java.time.LocalDateTime

@Service
class NotificationService(
  private val eventRepository: EventRepository,
  private val emailPreferencesRepository: EmailNotificationPreferencesRepository,
) {

  /**
   * NOT-003: Returns the user's own events plus global (username=null) events.
   * NOT-004: Filtered by rule codes (multiple values).
   * NOT-005: Filtered by triggered_at range (gte/lt).
   * NOT-006: Ordered by triggered_at desc, then id asc.
   */
  @Transactional(readOnly = true)
  fun listEvents(
    username: String,
    rules: List<String>?,
    triggeredAtGte: LocalDateTime?,
    triggeredAtLt: LocalDateTime?,
  ): List<Event> {
    var spec: Specification<Event> = EventSpecifications.visibleToUser(username)
      .and(EventSpecifications.fetchAssociations())
    if (!rules.isNullOrEmpty()) {
      spec = spec.and(EventSpecifications.ruleIn(rules))
    }
    if (triggeredAtGte != null) {
      spec = spec.and(EventSpecifications.triggeredAtGte(triggeredAtGte))
    }
    if (triggeredAtLt != null) {
      spec = spec.and(EventSpecifications.triggeredAtLt(triggeredAtLt))
    }
    return eventRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "triggeredAt").and(Sort.by(Sort.Direction.ASC, "id")))
  }

  /**
   * NOT-007: Returns date pagination info — oldest/newest dates with events + count of distinct dates.
   */
  fun getEventPages(username: String, rules: List<String>?, offset: Int, limit: Int): Triple<String?, String?, Int> {
    val rulesParam = rules?.takeIf { it.isNotEmpty() }?.joinToString(",")
    val allDates = eventRepository.findDistinctDatesPaged(username = username, rules = rulesParam)
    val count = allDates.size
    val paged = allDates.drop(offset).take(limit)
    val newest = paged.firstOrNull()?.toString()
    val oldest = paged.lastOrNull()?.toString()
    return Triple(newest, oldest, count)
  }

  /**
   * NOT-010: Returns the user's email notification frequency, defaulting to 'never' if not set.
   */
  fun getEmailFrequency(username: String): String = emailPreferencesRepository.findByUsername(username)?.frequency?.value
    ?: EmailFrequency.NEVER.value

  /**
   * NOT-011 and NOT-012: Sets the user's email frequency using update-or-create semantics.
   */
  @Transactional
  fun setEmailFrequency(username: String, frequency: EmailFrequency) {
    val existing = emailPreferencesRepository.findByUsername(username)
    if (existing != null) {
      existing.frequency = frequency
      emailPreferencesRepository.save(existing)
    } else {
      emailPreferencesRepository.save(
        EmailNotificationPreferences(username = username, frequency = frequency),
      )
    }
  }
}
