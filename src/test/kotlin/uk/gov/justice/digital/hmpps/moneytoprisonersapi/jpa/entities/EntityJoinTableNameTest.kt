package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.JoinTable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Verifies that every @JoinTable name on a ManyToMany relationship matches the corresponding
 * table name in the existing Django-managed PostgreSQL database.
 *
 * Django auto-generates join table names as  {owning_table}_{field_name},
 * e.g. the `credits` field on `credit_processingbatch` → `credit_processingbatch_credits`.
 *
 * Note: join tables that are entirely new (no Django equivalent) are excluded and must be
 * tracked separately as additions.
 */
@DisplayName("@JoinTable → Django join-table name alignment")
class EntityJoinTableNameTest {

  @ParameterizedTest(name = "{2}: @JoinTable on {0}.{1} must match Django name")
  @MethodSource("joinTableMappings")
  fun `join table maps to correct Django name`(
    entityClass: Class<*>,
    fieldName: String,
    expectedTableName: String,
  ) {
    val field = entityClass.declaredFields.firstOrNull { it.name == fieldName }
      ?: error("Field '$fieldName' not found on ${entityClass.simpleName}")
    field.isAccessible = true
    val joinTable = field.getAnnotation(JoinTable::class.java)
    assertNotNull(joinTable, "${entityClass.simpleName}.$fieldName has no @JoinTable annotation")
    assertEquals(
      expectedTableName,
      joinTable!!.name,
      "${entityClass.simpleName}.$fieldName: expected Django join table '$expectedTableName' but found '${joinTable.name}'",
    )
  }

  companion object {
    @JvmStatic
    fun joinTableMappings(): Stream<Arguments> = Stream.of(
      // prison_prison ↔ prison_category  (already correct — verifies it stays correct)
      Arguments.of(Prison::class.java, "categories", "prison_prison_categories"),
      // prison_prison ↔ prison_population (already correct — verifies it stays correct)
      Arguments.of(Prison::class.java, "populations", "prison_prison_populations"),

      // credit_processingbatch ↔ credit_credit
      // Django: credit_processingbatch.credits M2M → credit_processingbatch_credits
      Arguments.of(Batch::class.java, "credits", "credit_processingbatch_credits"),

      // credit_privateestatebatch ↔ credit_credit
      // Django: credit_privateestatebatch.credits M2M → credit_privateestatebatch_credits
      Arguments.of(PrivateEstateBatch::class.java, "credits", "credit_privateestatebatch_credits"),

      // mtp_auth_prisonusermapping user ↔ prison_prison
      // Django: mtp_auth_prisonusermapping.prisons M2M → mtp_auth_prisonusermapping_prisons
      Arguments.of(MtpUser::class.java, "prisons", "mtp_auth_prisonusermapping_prisons"),
    )
  }
}
