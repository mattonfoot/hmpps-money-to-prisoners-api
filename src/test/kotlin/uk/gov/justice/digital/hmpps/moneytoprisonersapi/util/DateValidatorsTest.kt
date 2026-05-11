package uk.gov.justice.digital.hmpps.moneytoprisonersapi.util

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Mirrors Python's `core/tests/test_models.py::TestValidateMonday` —
 * validateMonday accepts Mondays and rejects every other weekday.
 */
@DisplayName("validateMonday")
class DateValidatorsTest {

  @Test
  fun `when not Monday raises validation error`() {
    val tuesday = LocalDate.of(2021, 6, 22)
    assertThatThrownBy { validateMonday(tuesday) }
      .isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `when Monday does not raise error`() {
    val monday = LocalDate.of(2021, 6, 21)
    assertThatCode { validateMonday(monday) }.doesNotThrowAnyException()
  }

  @Test
  fun `every non-Monday weekday is rejected`() {
    // 2024-01-08 is a Monday — increment through the week.
    val monday = LocalDate.of(2024, 1, 8)
    (1L..6L).forEach { offset ->
      val date = monday.plusDays(offset)
      assertThatThrownBy { validateMonday(date) }
        .isInstanceOf(IllegalArgumentException::class.java)
    }
  }
}
