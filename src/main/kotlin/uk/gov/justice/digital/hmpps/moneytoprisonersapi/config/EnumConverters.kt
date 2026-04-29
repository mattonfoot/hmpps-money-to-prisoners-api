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
  }
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
