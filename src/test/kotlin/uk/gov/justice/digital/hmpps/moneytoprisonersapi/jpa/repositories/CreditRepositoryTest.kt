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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.AuthUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrison
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

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

  /** credit_credit has nullable FKs to prison and auth_user; persist parents first when used. */
  private fun seedPrison(nomisId: String, name: String = "HMP $nomisId"): PrisonPrison = entityManager.persist(
    PrisonPrison().apply {
      this.nomisId = nomisId
      this.name = name
      this.generalLedgerCode = ""
      this.region = "Region"
      this.cmsEstablishmentCode = ""
    },
  )

  private fun seedUser(username: String): AuthUser = entityManager.persist(
    AuthUser().apply {
      this.username = username
      this.email = "$username@mtp.local"
      this.password = "!unusable"
      this.firstName = ""
      this.lastName = ""
    },
  )

  private fun newCredit(
    amount: Long = 1000,
    prisonerNumber: String? = "A1234BC",
    prisonerName: String? = "John Smith",
    prisonerDob: LocalDate? = LocalDate.of(1990, 1, 15),
    prison: PrisonPrison? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
    blocked: Boolean = false,
    reviewed: Boolean = false,
    reconciled: Boolean = false,
    receivedAt: OffsetDateTime? = null,
    owner: AuthUser? = null,
  ): Credit = Credit().apply {
    this.amount = amount
    this.prisonerNumber = prisonerNumber
    this.prisonerName = prisonerName
    this.prisonerDob = prisonerDob
    this.prison = prison
    this.resolution = resolution.value
    this.blocked = blocked
    this.reviewed = reviewed
    this.reconciled = reconciled
    this.receivedAt = receivedAt
    this.owner = owner
  }

  @Nested
  @DisplayName("Save and retrieve")
  inner class SaveAndRetrieve {

    @Test
    fun `timestamps are auto-populated on save`() {
      val saved = creditRepository.save(newCredit())

      assertNotNull(saved.created)
      assertNotNull(saved.modified)
      assertNotNull(saved.id)
    }

    @Test
    fun `all fields are persisted correctly`() {
      val prison = seedPrison("LEI", "HMP Leeds")
      val owner = seedUser("clerk1")
      val receivedAt = OffsetDateTime.of(2024, 3, 15, 10, 30, 0, 0, ZoneOffset.UTC)

      val saved = creditRepository.save(
        newCredit(
          amount = 5000,
          prisonerNumber = "B5678DE",
          prisonerName = "Jane Doe",
          prisonerDob = LocalDate.of(1985, 6, 20),
          prison = prison,
          resolution = CreditResolution.PENDING,
          blocked = true,
          reviewed = true,
          reconciled = true,
          receivedAt = receivedAt,
          owner = owner,
        ),
      )
      entityManager.flush()
      entityManager.clear()

      val found = creditRepository.findById(saved.id!!).orElseThrow()
      assertEquals(5000L, found.amount)
      assertEquals("B5678DE", found.prisonerNumber)
      assertEquals("Jane Doe", found.prisonerName)
      assertEquals(LocalDate.of(1985, 6, 20), found.prisonerDob)
      assertEquals("LEI", found.prison?.nomisId)
      assertEquals(CreditResolution.PENDING.value, found.resolution)
      assertTrue(found.blocked)
      assertTrue(found.reviewed)
      assertTrue(found.reconciled)
      assertEquals(receivedAt, found.receivedAt)
      assertEquals("clerk1", found.owner?.username)
    }
  }

  @Nested
  @DisplayName("CRD-010: Default query excludes initial and failed")
  inner class CompletedCreditsQuery {

    @Test
    fun `findByResolutionNotIn excludes initial and failed`() {
      val excluded = listOf(CreditResolution.INITIAL.value, CreditResolution.FAILED.value)
      val countBefore = creditRepository.findByResolutionNotIn(excluded).size

      creditRepository.save(newCredit(amount = 100, resolution = CreditResolution.INITIAL))
      creditRepository.save(newCredit(amount = 200, resolution = CreditResolution.PENDING))
      creditRepository.save(newCredit(amount = 300, resolution = CreditResolution.CREDITED))
      creditRepository.save(newCredit(amount = 400, resolution = CreditResolution.FAILED))
      creditRepository.save(newCredit(amount = 500, resolution = CreditResolution.REFUNDED))
      creditRepository.save(newCredit(amount = 600, resolution = CreditResolution.MANUAL))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByResolutionNotIn(excluded)

      assertEquals(countBefore + 4, results.size)
      assertTrue(results.none { it.resolution == CreditResolution.INITIAL.value })
      assertTrue(results.none { it.resolution == CreditResolution.FAILED.value })
    }
  }

  @Nested
  @DisplayName("CRD-011: objects_all includes all resolutions")
  inner class AllCreditsQuery {

    @Test
    fun `findAll returns all credits regardless of resolution`() {
      val countBeforeInsert = creditRepository.findAll().size

      CreditResolution.entries.forEach { resolution ->
        creditRepository.save(newCredit(amount = 100, resolution = resolution))
      }
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findAll()
      assertEquals(countBeforeInsert + CreditResolution.entries.size, results.size)
    }
  }

  @Nested
  @DisplayName("Find by resolution")
  inner class FindByResolution {

    @Test
    fun `findByResolution returns only matching credits`() {
      val countBefore = creditRepository.findByResolution(CreditResolution.PENDING.value).size

      creditRepository.save(newCredit(amount = 100, resolution = CreditResolution.PENDING))
      creditRepository.save(newCredit(amount = 200, resolution = CreditResolution.CREDITED))
      creditRepository.save(newCredit(amount = 300, resolution = CreditResolution.PENDING))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByResolution(CreditResolution.PENDING.value)
      assertEquals(countBefore + 2, results.size)
      assertTrue(results.all { it.resolution == CreditResolution.PENDING.value })
    }
  }

  @Nested
  @DisplayName("Find by prison")
  inner class FindByPrison {

    @Test
    fun `findByPrison returns credits for specific prison`() {
      val lei = seedPrison("LEI")
      val mdi = seedPrison("MDI")
      creditRepository.save(newCredit(amount = 100, prison = lei))
      creditRepository.save(newCredit(amount = 200, prison = mdi))
      creditRepository.save(newCredit(amount = 300, prison = lei))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByPrison(lei)
      assertEquals(2, results.size)
      assertTrue(results.all { it.prison?.nomisId == "LEI" })
    }

    @Test
    fun `findByPrisonIsNull returns credits with no prison`() {
      val lei = seedPrison("LEI")
      creditRepository.save(newCredit(amount = 100, prison = null))
      creditRepository.save(newCredit(amount = 200, prison = lei))
      creditRepository.save(newCredit(amount = 300, prison = null))
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
      creditRepository.save(newCredit(amount = 100, blocked = true))
      creditRepository.save(newCredit(amount = 200, blocked = false))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByBlocked(true)
      assertEquals(1, results.size)
      assertTrue(results[0].blocked)
    }

    @Test
    fun `findByReviewed returns reviewed credits`() {
      creditRepository.save(newCredit(amount = 100, reviewed = true))
      creditRepository.save(newCredit(amount = 200, reviewed = false))
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
    fun `findByOwnerUsername returns credits for specific owner`() {
      val clerk1 = seedUser("clerk1")
      val clerk2 = seedUser("clerk2")
      creditRepository.save(newCredit(amount = 100, owner = clerk1))
      creditRepository.save(newCredit(amount = 200, owner = clerk2))
      creditRepository.save(newCredit(amount = 300, owner = clerk1))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByOwnerUsername("clerk1")
      assertEquals(2, results.size)
      assertTrue(results.all { it.owner?.username == "clerk1" })
    }
  }

  @Nested
  @DisplayName("Find by received_at range")
  inner class FindByReceivedAtRange {

    @Test
    fun `findByReceivedAtGreaterThanEqualAndReceivedAtBefore returns credits in date range`() {
      val t1 = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
      val t2 = OffsetDateTime.of(2024, 3, 15, 10, 0, 0, 0, ZoneOffset.UTC)
      val t3 = OffsetDateTime.of(2024, 6, 30, 10, 0, 0, 0, ZoneOffset.UTC)

      creditRepository.save(newCredit(amount = 100, receivedAt = t1))
      creditRepository.save(newCredit(amount = 200, receivedAt = t2))
      creditRepository.save(newCredit(amount = 300, receivedAt = t3))
      entityManager.flush()
      entityManager.clear()

      val results = creditRepository.findByReceivedAtGreaterThanEqualAndReceivedAtBefore(
        OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC),
        OffsetDateTime.of(2024, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC),
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
      creditRepository.save(newCredit(amount = 100, prisonerNumber = "A1234BC"))
      creditRepository.save(newCredit(amount = 200, prisonerNumber = "B5678DE"))
      creditRepository.save(newCredit(amount = 300, prisonerNumber = "A1234BC"))
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
      creditRepository.save(newCredit(prisonerNumber = "A1234BC", resolution = CreditResolution.PENDING))
      entityManager.flush()

      assertTrue(
        creditRepository.existsByPrisonerNumberAndResolution(
          "A1234BC",
          CreditResolution.PENDING.value,
        ),
      )
    }

    @Test
    fun `existsByPrisonerNumberAndResolution returns false when no match`() {
      creditRepository.save(newCredit(prisonerNumber = "A1234BC", resolution = CreditResolution.PENDING))
      entityManager.flush()

      assertFalse(
        creditRepository.existsByPrisonerNumberAndResolution(
          "A1234BC",
          CreditResolution.CREDITED.value,
        ),
      )
    }
  }
}
