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
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.UserEvent
import java.time.LocalDateTime

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

  @Nested
  @DisplayName("UEL-006: Ordered by timestamp desc, pk desc")
  inner class Ordering {

    @Test
    fun `findAllByOrderByTimestampDescIdDesc returns events most recent first`() {
      val older = UserEvent(path = "/older/").apply { timestamp = LocalDateTime.of(2024, 1, 1, 10, 0, 0) }
      val middle = UserEvent(path = "/middle/").apply { timestamp = LocalDateTime.of(2024, 6, 15, 12, 0, 0) }
      val newest = UserEvent(path = "/newest/").apply { timestamp = LocalDateTime.of(2024, 12, 31, 23, 59, 0) }

      userEventRepository.save(middle)
      userEventRepository.save(older)
      userEventRepository.save(newest)
      entityManager.flush()

      val results = userEventRepository.findAllByOrderByTimestampDescIdDesc()

      assertEquals(3, results.size)
      assertEquals("/newest/", results[0].path)
      assertEquals("/middle/", results[1].path)
      assertEquals("/older/", results[2].path)
    }

    @Test
    fun `findAllByOrderByTimestampDescIdDesc breaks ties by id desc`() {
      val sameTime = LocalDateTime.of(2024, 6, 1, 12, 0, 0)
      val first = UserEvent(path = "/first/").apply { timestamp = sameTime }
      val second = UserEvent(path = "/second/").apply { timestamp = sameTime }

      val savedFirst = userEventRepository.save(first)
      val savedSecond = userEventRepository.save(second)
      entityManager.flush()

      val results = userEventRepository.findAllByOrderByTimestampDescIdDesc()

      // Both have same timestamp, so higher id comes first
      assertEquals(savedSecond.id, results[0].id)
      assertEquals(savedFirst.id, results[1].id)
    }
  }
}
