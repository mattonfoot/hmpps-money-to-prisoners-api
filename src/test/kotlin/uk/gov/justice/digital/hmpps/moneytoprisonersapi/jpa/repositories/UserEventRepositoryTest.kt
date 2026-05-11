package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.junit.jupiter.api.Assertions.assertEquals
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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserEvent
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DataJpaTest
@Import(ContainersConfig::class)
@DisplayName("UserEvent Repository")
class UserEventRepositoryTest @Autowired constructor(
  val userEventRepository: UserEventRepository,
  private val entityManager: TestEntityManager,
) {

  @BeforeEach
  fun setup() {
    userEventRepository.deleteAll()
    entityManager.clear()
  }

  /** user_event_log_userevent.user_id is NOT NULL — seed a stub AuthUser per test. */
  private fun seedUser(username: String = "test-user"): AuthUser = entityManager.persist(
    AuthUser().apply {
      this.username = username
      this.email = "$username@mtp.local"
      this.password = "!unusable"
      this.firstName = ""
      this.lastName = ""
    },
  )

  @Nested
  @DisplayName("UEL-006: Ordered by timestamp desc, pk desc")
  inner class Ordering {

    @Test
    fun `findAllByOrderByTimestampDescIdDesc returns events most recent first`() {
      val user = seedUser()
      val older = UserEvent(apiUrlPath = "/older/").apply {
        timestamp = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        this.user = user
      }
      val middle = UserEvent(apiUrlPath = "/middle/").apply {
        timestamp = OffsetDateTime.of(2024, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC)
        this.user = user
      }
      val newest = UserEvent(apiUrlPath = "/newest/").apply {
        timestamp = OffsetDateTime.of(2024, 12, 31, 23, 59, 0, 0, ZoneOffset.UTC)
        this.user = user
      }

      userEventRepository.save(middle)
      userEventRepository.save(older)
      userEventRepository.save(newest)
      entityManager.flush()

      val results = userEventRepository.findAllByOrderByTimestampDescIdDesc()

      assertEquals(3, results.size)
      assertEquals("/newest/", results[0].apiUrlPath)
      assertEquals("/middle/", results[1].apiUrlPath)
      assertEquals("/older/", results[2].apiUrlPath)
    }

    @Test
    fun `findAllByOrderByTimestampDescIdDesc breaks ties by id desc`() {
      val user = seedUser()
      val sameTime = OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC)
      val first = UserEvent(apiUrlPath = "/first/").apply {
        timestamp = sameTime
        this.user = user
      }
      val second = UserEvent(apiUrlPath = "/second/").apply {
        timestamp = sameTime
        this.user = user
      }

      val savedFirst = userEventRepository.save(first)
      val savedSecond = userEventRepository.save(second)
      entityManager.flush()

      val results = userEventRepository.findAllByOrderByTimestampDescIdDesc()

      // Equal timestamps — the larger id sorts first.
      assertEquals(savedSecond.id, results[0].id)
      assertEquals(savedFirst.id, results[1].id)
    }
  }
}
