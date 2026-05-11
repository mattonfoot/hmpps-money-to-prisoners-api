package uk.gov.justice.digital.hmpps.moneytoprisonersapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CheckStatus
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditSource
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementMethod
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.DisbursementResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.services.CreditStatus

/**
 * Registers Spring MVC converters that allow lowercase enum values in query parameters
 * (matching Django's convention) to be mapped to Kotlin uppercase enum constants.
 */
@Configuration
class EnumConverters : WebMvcConfigurer {
  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(StringToCreditResolution())
    registry.addConverter(StringToCreditSource())
    registry.addConverter(StringToCreditStatus())
    registry.addConverter(StringToDisbursementResolution())
    registry.addConverter(StringToDisbursementMethod())
    registry.addConverter(StringToCheckStatus())
    registry.addConverter(StringToOffsetDateTime())
    registry.addConverter(StringToLocalDate())
  }
}

/**
 * Accepts both naive (`2024-06-15T12:00:00`, `2024-06-15`) and offset-bearing
 * (`2024-06-15T12:00:00Z`, `2024-06-15T12:00:00+01:00`) ISO datetimes. Naive
 * datetimes default to UTC — matching Django REST Framework's behaviour when
 * `USE_TZ=True` and the input has no offset.
 */
class StringToOffsetDateTime : Converter<String, java.time.OffsetDateTime> {
  override fun convert(source: String): java.time.OffsetDateTime {
    val s = source.trim()
    return runCatching { java.time.OffsetDateTime.parse(s) }
      .recoverCatching {
        java.time.LocalDateTime.parse(s).atOffset(java.time.ZoneOffset.UTC)
      }
      .recoverCatching {
        java.time.LocalDate.parse(s).atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
      }
      .getOrThrow()
  }
}

class StringToLocalDate : Converter<String, java.time.LocalDate> {
  override fun convert(source: String): java.time.LocalDate = java.time.LocalDate.parse(source.trim())
}

class StringToCreditResolution : Converter<String, CreditResolution> {
  override fun convert(source: String): CreditResolution = CreditResolution.fromValue(source)
}

class StringToCreditSource : Converter<String, CreditSource> {
  override fun convert(source: String): CreditSource = CreditSource.fromValue(source)
}

class StringToCreditStatus : Converter<String, CreditStatus> {
  override fun convert(source: String): CreditStatus = CreditStatus.entries.first { it.name.equals(source, ignoreCase = true) }
}

class StringToDisbursementResolution : Converter<String, DisbursementResolution> {
  override fun convert(source: String): DisbursementResolution = DisbursementResolution.fromValue(source)
}

class StringToDisbursementMethod : Converter<String, DisbursementMethod> {
  override fun convert(source: String): DisbursementMethod = DisbursementMethod.fromValue(source)
}

class StringToCheckStatus : Converter<String, CheckStatus> {
  override fun convert(source: String): CheckStatus = CheckStatus.fromValue(source)
}
