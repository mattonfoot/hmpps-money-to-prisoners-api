package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(
  name = "security_bankaccount",
  schema = "public",
  uniqueConstraints = [
    UniqueConstraint(
      name = "security_bankaccount_sort_code_account_number_d991aee8_uniq",
      columnNames = [
        "sort_code",
        "account_number",
        "roll_number",
      ],
    ),
  ],
)
open class SecurityBankaccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  open var id: Long? = null

  @Size(max = 50)
  @NotNull
  @Column(name = "sort_code", nullable = false, length = 50)
  open var sortCode: String = ""

  @Size(max = 50)
  @NotNull
  @Column(name = "account_number", nullable = false, length = 50)
  open var accountNumber: String = ""

  @Size(max = 50)
  @NotNull
  @Column(name = "roll_number", nullable = false, length = 50)
  open var rollNumber: String = ""
}
