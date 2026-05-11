package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "account_balance", schema = "public")
open class AccountBalance {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @NotNull
  @Column(name = "created", nullable = false)
  open var created: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "modified", nullable = false)
  open var modified: OffsetDateTime = OffsetDateTime.now()

  @NotNull
  @Column(name = "closing_balance", nullable = false)
  open var closingBalance: Long = 0L

  @NotNull
  @Column(name = "date", nullable = false)
  open var date: LocalDate = LocalDate.now()

  /**
   * Mirrors Django `Balance.__str__`: `<YYYY-MM-DD> £<pounds.pence>`.
   * Closing balance is stored as integer pence.
   */
  override fun toString(): String {
    val pounds = closingBalance / 100
    val pence = (closingBalance % 100).let { if (it < 0) -it else it }
    return "%s £%d.%02d".format(date, pounds, pence)
  }
}
