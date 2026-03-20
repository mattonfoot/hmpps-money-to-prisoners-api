package uk.gov.justice.digital.hmpps.moneytoprisonersapi.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

/**
 * Mirrors Python's FlexibleDjangoJSONEncoder (UEL-004, UEL-005).
 *
 * Encodes arbitrary objects to a JSON string and never throws:
 * - DateTimes are serialized as ISO 8601 strings with timezone info (UEL-005).
 * - If standard serialization fails, falls back to the object's `id` property
 *   (analogous to Django's `.pk`) or finally to [toString] (UEL-004).
 */
object FlexibleJsonEncoder {

  private val mapper: ObjectMapper = ObjectMapper().apply {
    registerModule(JavaTimeModule())
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
  }

  fun encode(value: Any?): String {
    if (value == null) return "null"
    return try {
      mapper.writeValueAsString(value)
    } catch (_: Exception) {
      // pk fallback: use id property if available, otherwise toString()
      val id = runCatching {
        value::class.members.firstOrNull { it.name == "id" }?.call(value)
      }.getOrNull()
      if (id != null) {
        mapper.writeValueAsString(id)
      } else {
        mapper.writeValueAsString(value.toString())
      }
    }
  }
}
