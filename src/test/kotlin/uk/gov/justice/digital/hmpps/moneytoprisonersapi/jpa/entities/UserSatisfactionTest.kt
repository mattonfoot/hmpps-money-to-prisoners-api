package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("UserSatisfaction entity")
class UserSatisfactionTest {

  private fun makeUserSatisfaction(
    rated1: Int = 0,
    rated2: Int = 0,
    rated3: Int = 0,
    rated4: Int = 0,
    rated5: Int = 0,
  ) = UserSatisfaction(
    date = LocalDate.of(2024, 3, 1),
    rated1 = rated1,
    rated2 = rated2,
    rated3 = rated3,
    rated4 = rated4,
    rated5 = rated5,
  )

  // -------------------------------------------------------------------------
  // PRF-010: Daily ratings rated_1 through rated_5
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-010 stores all five rating fields`() {
    val us = makeUserSatisfaction(rated1 = 1, rated2 = 2, rated3 = 3, rated4 = 4, rated5 = 5)
    assertThat(us.rated1).isEqualTo(1)
    assertThat(us.rated2).isEqualTo(2)
    assertThat(us.rated3).isEqualTo(3)
    assertThat(us.rated4).isEqualTo(4)
    assertThat(us.rated5).isEqualTo(5)
  }

  // -------------------------------------------------------------------------
  // PRF-011: Date is primary key
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-011 date is the identifier field`() {
    val us = makeUserSatisfaction()
    assertThat(us.date).isEqualTo(LocalDate.of(2024, 3, 1))
  }

  // -------------------------------------------------------------------------
  // PRF-012: Percentage satisfied = (rated_4 + rated_5) / total
  // -------------------------------------------------------------------------

  @Nested
  @DisplayName("percentageSatisfied (PRF-012)")
  inner class PercentageSatisfied {

    @Test
    fun `PRF-012 returns correct ratio for mixed ratings`() {
      // rated_4 + rated_5 = 4 + 6 = 10, total = 1+2+3+4+6 = 16
      val us = makeUserSatisfaction(rated1 = 1, rated2 = 2, rated3 = 3, rated4 = 4, rated5 = 6)
      assertThat(us.percentageSatisfied).isEqualTo(10.0 / 16.0)
    }

    @Test
    fun `PRF-012 returns 1_0 when all rated 4 or 5`() {
      val us = makeUserSatisfaction(rated4 = 3, rated5 = 7)
      assertThat(us.percentageSatisfied).isEqualTo(1.0)
    }

    @Test
    fun `PRF-012 returns 0_0 when all rated 1 to 3`() {
      val us = makeUserSatisfaction(rated1 = 5, rated2 = 3, rated3 = 2)
      assertThat(us.percentageSatisfied).isEqualTo(0.0)
    }

    @Test
    fun `PRF-012 returns null when total is zero`() {
      val us = makeUserSatisfaction()
      assertThat(us.percentageSatisfied).isNull()
    }
  }

  // -------------------------------------------------------------------------
  // PRF-013: Mean percentage satisfied — via repository
  // -------------------------------------------------------------------------

  @Test
  fun `PRF-013 total sums all five ratings`() {
    val us = makeUserSatisfaction(rated1 = 1, rated2 = 1, rated3 = 1, rated4 = 1, rated5 = 1)
    assertThat(us.total).isEqualTo(5)
  }
}
