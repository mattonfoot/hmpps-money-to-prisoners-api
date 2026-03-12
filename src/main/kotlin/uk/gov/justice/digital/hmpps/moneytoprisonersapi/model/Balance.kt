package uk.gov.justice.digital.hmpps.moneytoprisonersapi.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "balance")
class Balance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val closingBalance: BigInteger,

    @Column(nullable = false, unique = true)
    val date: LocalDate,

    @Column(nullable = false, updatable = false)
    var created: LocalDateTime? = null,

    @Column(nullable = false)
    var modified: LocalDateTime? = null,
) {

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        created = now
        modified = now
    }

    @PreUpdate
    fun onUpdate() {
        modified = LocalDateTime.now()
    }

    override fun toString(): String {
        val pounds = closingBalance / BigInteger.valueOf(100)
        val pence = (closingBalance % BigInteger.valueOf(100)).abs()
        return "$date £$pounds.${pence.toString().padStart(2, '0')}"
    }
}
