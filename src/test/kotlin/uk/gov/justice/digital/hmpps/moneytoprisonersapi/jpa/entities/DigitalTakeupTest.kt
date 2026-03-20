package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("DigitalTakeup entity")
class DigitalTakeupTest {

  private val prison = Prison(nomisId = "TST")
  private val date = LocalDate.of(2024, 1, 15)

  private fun makeDigitalTakeup(
    creditsByPost: Int = 0,
    creditsByMtp: Int = 0,
  ) = DigitalTakeup(
    date = date,
    prison = prison,
    creditsByPost = creditsByPost,
    creditsByMtp = creditsByMtp,
  )

  // -------------------------------------------------------------------------
  // PRF-002: Tracks credits_by_post and credits_by_mtp (Integer counts)
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-002 stores credits_by_post as integer`() {
    val dt = makeDigitalTakeup(creditsByPost = 42)
    assertThat(dt.creditsByPost).isEqualTo(42)
  }

  @Test
  fun `PRF-002 stores credits_by_mtp as integer`() {
    val dt = makeDigitalTakeup(creditsByMtp = 17)
    assertThat(dt.creditsByMtp).isEqualTo(17)
  }

  // -------------------------------------------------------------------------
  // PRF-001: Unique on (date, prison)
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-001 holds date field`() {
    val dt = makeDigitalTakeup()
    assertThat(dt.date).isEqualTo(LocalDate.of(2024, 1, 15))
  }

  @Test
  fun `PRF-001 holds prison reference`() {
    val dt = makeDigitalTakeup()
    assertThat(dt.prison.nomisId).isEqualTo("TST")
  }

  // -------------------------------------------------------------------------
  // PRF-003: Calculated digital_takeup = mtp / (post + mtp)
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("digitalTakeup computed property (PRF-003)")
  inner class DigitalTakeupProperty {

    @Test
    fun `PRF-003 returns ratio when both are non-zero`() {
      val dt = makeDigitalTakeup(creditsByPost = 3, creditsByMtp = 7)
      assertThat(dt.digitalTakeup).isEqualTo(7.0 / 10.0)
    }

    @Test
    fun `PRF-003 returns 1_0 when all credits are by MTP`() {
      val dt = makeDigitalTakeup(creditsByPost = 0, creditsByMtp = 5)
      assertThat(dt.digitalTakeup).isEqualTo(1.0)
    }

    @Test
    fun `PRF-003 returns 0_0 when no credits by MTP`() {
      val dt = makeDigitalTakeup(creditsByPost = 10, creditsByMtp = 0)
      assertThat(dt.digitalTakeup).isEqualTo(0.0)
    }

    @Test
    fun `PRF-003 returns null when total is zero`() {
      val dt = makeDigitalTakeup(creditsByPost = 0, creditsByMtp = 0)
      assertThat(dt.digitalTakeup).isNull()
    }
  }
}
