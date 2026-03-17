package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.ContainersConfig
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@Import(ContainersConfig::class)
@DisplayName("Credit Repository")
class CreditRepositoryTest @Autowired constructor(
  val creditRepository: CreditRepository,
  private val entityManager: TestEntityManager,
) {

  @BeforeEach
  fun setup() {
    creditRepository.deleteAll()
    entityManager.clear()
  }

  private fun createCredit(
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prisonerName: String? = "John Smith",
    prisonerDob: LocalDate? = LocalDate.of(1990, 1, 15),
    prison: String? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    reviewed: Boolean = false,
    reconciled: Boolean = false,
    receivedAt: LocalDateTime? = null,
    owner: String? = null,
  ): Credit = Credit(
    amount = amount,
    prisonerNumber = prisonerNumber,
    prisonerName = prisonerName,
    prisonerDob = prisonerDob,
    prison = prison,
    resolution = resolution,
    blocked = blocked,
    reviewed = reviewed,
    reconciled = reconciled,
    receivedAt = receivedAt,
    owner = owner,
  )

  @Nested
  @DisplayName("Save and retrieve")
  inner class SaveAndRetrieve {

    @Test
    fun `timestamps are auto-populated on save`() {
      val credit = createCredit()
      val saved = creditRepository.save(credit)

      assertNotNull(saved.created)
      assertNotNull(saved.modified)
      assertNotNull(saved.id)
    }

    @Test
    fun `all fields are persisted correctly`() {
      val receivedAt = LocalDateTime.of(2024, 3, 15, 10, 30, 0)
      val credit = createCredit(
        amount = 5000,
        prisonerNumber = "B5678DE",
        prisonerName = "Jane Doe",
        prisonerDob = LocalDate.of(1985, 6, 20),
        prison = "LEI",
        resolution = CreditResolution.PENDING,
        blocked = true,
        reviewed = true,
        reconciled = true,
        receivedAt = receivedAt,
        owner = "clerk1",
      )

      val saved = creditRepository.save(credit)
      entityManager.flush()
      entityManager.clear()

      val found = creditRepository.findById(saved.id!!).orElseThrow()
      assertEquals(5000L, found.amount)
      assertEquals("B5678DE", found.prisonerNumber)
      assertEquals("Jane Doe", found.prisonerName)
      assertEquals(LocalDate.of(1985, 6, 20), found.prisonerDob)
      assertEquals("LEI", found.prison)
      assertEquals(CreditResolution.PENDING, found.resolution)
      assertTrue(found.blocked)
      assertTrue(found.reviewed)
      assertTrue(found.reconciled)
      assertEquals(receivedAt, found.receivedAt)
      assertEquals("clerk1", found.owner)
    }
  }

  @Nested
  @DisplayName("CRD-010: Default query excludes initial and failed")
  inner class CompletedCreditsQuery {

    @Test
    fun `findByResolutionNotIn excludes initial and failed`() {
      creditRepository.save(createCredit(amount = 100, resolution = CreditResolution.INITIAL))
      creditRepository.save(createCredit(amount = 200, resolution = CreditResolution.PENDING))
      creditRepository.save(createCredit(amount = 300, resolution = CreditResolution.CREDITED))
      creditRepository.save(createCredit(amount = 400, resolution = CreditResolution.FAILED))
      creditRepository.save(createCredit(amount = 500, resolution = CreditResolution.REFUNDED))
      creditRepository.save(createCredit(amount = 600, resolution = CreditResolution.MANUAL))
      entityManager.flush()
      entityManager.clear()

      val excluded = listOf(CreditResolution.INITIAL, CreditResolution.FAILED)
      val results = creditRepository.findByResolutionNotIn(excluded)

      assertEquals(4, results.size)
      assertTrue(results.none { it.resolution == CreditResolution.INITIAL })
      assertTrue(results.none { it.resolution == CreditResolution.FAILED })
    }
  }

  @Nested
  @DisplayName("CRD-011: objects_all includes all resolutions")
  inner class AllCreditsQuery {

    @Test
    fun `findAll returns all credits regardless of resolution`() {
      CreditResolution.entries.forEach { resolution ->
        creditRepository.save(createCredit(amount = 100, resolution = resolution))
      }
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findAll()
      assertEquals(CreditResolution.entries.size, results.size)
    }
  }

  @Nested
  @DisplayName("Find by resolution")
  inner class FindByResolution {

    @Test
    fun `findByResolution returns only matching credits`() {
      creditRepository.save(createCredit(amount = 100, resolution = CreditResolution.PENDING))
      creditRepository.save(createCredit(amount = 200, resolution = CreditResolution.CREDITED))
      creditRepository.save(createCredit(amount = 300, resolution = CreditResolution.PENDING))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByResolution(CreditResolution.PENDING)
      assertEquals(2, results.size)
      assertTrue(results.all { it.resolution == CreditResolution.PENDING })
    }
  }

  @Nested
  @DisplayName("Find by prison")
  inner class FindByPrison {

    @Test
    fun `findByPrison returns credits for specific prison`() {
      creditRepository.save(createCredit(amount = 100, prison = "LEI"))
      creditRepository.save(createCredit(amount = 200, prison = "MDI"))
      creditRepository.save(createCredit(amount = 300, prison = "LEI"))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByPrison("LEI")
      assertEquals(2, results.size)
      assertTrue(results.all { it.prison == "LEI" })
    }

    @Test
    fun `findByPrisonIsNull returns credits with no prison`() {
      creditRepository.save(createCredit(amount = 100, prison = null))
      creditRepository.save(createCredit(amount = 200, prison = "LEI"))
      creditRepository.save(createCredit(amount = 300, prison = null))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByPrisonIsNull()
      assertEquals(2, results.size)
      assertTrue(results.all { it.prison == null })
    }
  }

  @Nested
  @DisplayName("Find by blocked and reviewed")
  inner class FindByFlags {

    @Test
    fun `findByBlocked returns blocked credits`() {
      creditRepository.save(createCredit(amount = 100, blocked = true))
      creditRepository.save(createCredit(amount = 200, blocked = false))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByBlocked(true)
      assertEquals(1, results.size)
      assertTrue(results[0].blocked)
    }

    @Test
    fun `findByReviewed returns reviewed credits`() {
      creditRepository.save(createCredit(amount = 100, reviewed = true))
      creditRepository.save(createCredit(amount = 200, reviewed = false))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByReviewed(true)
      assertEquals(1, results.size)
      assertTrue(results[0].reviewed)
    }
  }

  @Nested
  @DisplayName("Find by owner")
  inner class FindByOwner {

    @Test
    fun `findByOwner returns credits for specific owner`() {
      creditRepository.save(createCredit(amount = 100, owner = "clerk1"))
      creditRepository.save(createCredit(amount = 200, owner = "clerk2"))
      creditRepository.save(createCredit(amount = 300, owner = "clerk1"))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByOwner("clerk1")
      assertEquals(2, results.size)
      assertTrue(results.all { it.owner == "clerk1" })
    }
  }

  @Nested
  @DisplayName("Find by received_at range")
  inner class FindByReceivedAtRange {

    @Test
    fun `findByReceivedAtBetween returns credits in date range`() {
      val t1 = LocalDateTime.of(2024, 1, 1, 10, 0)
      val t2 = LocalDateTime.of(2024, 3, 15, 10, 0)
      val t3 = LocalDateTime.of(2024, 6, 30, 10, 0)

      creditRepository.save(createCredit(amount = 100, receivedAt = t1))
      creditRepository.save(createCredit(amount = 200, receivedAt = t2))
      creditRepository.save(createCredit(amount = 300, receivedAt = t3))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByReceivedAtGreaterThanEqualAndReceivedAtBefore(
        LocalDateTime.of(2024, 2, 1, 0, 0),
        LocalDateTime.of(2024, 5, 1, 0, 0),
      )
      assertEquals(1, results.size)
      assertEquals(200L, results[0].amount)
    }
  }

  @Nested
  @DisplayName("Find by prisoner_number")
  inner class FindByPrisonerNumber {

    @Test
    fun `findByPrisonerNumber returns matching credits`() {
      creditRepository.save(createCredit(amount = 100, prisonerNumber = "A1234BC"))
      creditRepository.save(createCredit(amount = 200, prisonerNumber = "B5678DE"))
      creditRepository.save(createCredit(amount = 300, prisonerNumber = "A1234BC"))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByPrisonerNumber("A1234BC")
      assertEquals(2, results.size)
      assertTrue(results.all { it.prisonerNumber == "A1234BC" })
    }
  }

  @Nested
  @DisplayName("Existence checks")
  inner class ExistenceChecks {

    @Test
    fun `existsByPrisonerNumberAndResolution returns true when match exists`() {
      creditRepository.save(createCredit(prisonerNumber = "A1234BC", resolution = CreditResolution.PENDING))
      entityManager.flush()

      assertTrue(creditRepository.existsByPrisonerNumberAndResolution("A1234BC", CreditResolution.PENDING))
    }

    @Test
    fun `existsByPrisonerNumberAndResolution returns false when no match`() {
      creditRepository.save(createCredit(prisonerNumber = "A1234BC", resolution = CreditResolution.PENDING))
      entityManager.flush()

      assertFalse(creditRepository.existsByPrisonerNumberAndResolution("A1234BC", CreditResolution.CREDITED))
    }
  }
}
