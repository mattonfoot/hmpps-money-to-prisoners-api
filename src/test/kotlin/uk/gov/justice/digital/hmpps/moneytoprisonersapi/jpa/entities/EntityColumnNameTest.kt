package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Verifies that JPA entity field-to-column mappings match the Django database schema.
 * Django uses {field_name}_id for FK columns (e.g. prison_id, owner_id).
 */
@DisplayName("Entity column name alignment with Django schema")
class EntityColumnNameTest {

  @ParameterizedTest(name = "{0}.{1} must map to column {2}")
  @MethodSource("columnMappings")
  fun `entity field maps to correct Django column name`(
    entityClass: Class<*>,
    fieldName: String,
    expectedColumnName: String,
  ) {
    val field = entityClass.getDeclaredField(fieldName)
    // Foreign-key fields (@ManyToOne / @OneToOne) carry @JoinColumn instead of @Column.
    val joinColumn = field.getAnnotation(JoinColumn::class.java)
    val column = field.getAnnotation(Column::class.java)
    val actualColumnName =
      joinColumn?.name?.ifEmpty { fieldName }
        ?: column?.name?.ifEmpty { fieldName }
        ?: fieldName
    assertEquals(
      expectedColumnName,
      actualColumnName,
      "${entityClass.simpleName}.$fieldName: expected column '$expectedColumnName' but mapped to '$actualColumnName'",
    )
  }

  companion object {
    @JvmStatic
    fun columnMappings(): Stream<Arguments> = Stream.of(
      // FK columns — Django uses prison_id for FK to prison_prison
      Arguments.of(Credit::class.java, "prison", "prison_id"),
      Arguments.of(Disbursement::class.java, "prison", "prison_id"),
    )
  }
}
