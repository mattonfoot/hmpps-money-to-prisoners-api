package uk.gov.justice.digital.hmpps.moneytoprisonersapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUser
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Prison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.MtpUserRepository

@ExtendWith(MockitoExtension::class)
@DisplayName("PrisonUserMappingService")
class PrisonUserMappingServiceTest {

  @Mock
  private lateinit var mtpUserRepository: MtpUserRepository

  @InjectMocks
  private lateinit var prisonUserMappingService: PrisonUserMappingService

  private fun makePrison(id: String) = Prison(nomisId = id, name = "Prison $id")
  private fun makeUser(id: Long = 1L, username: String = "testuser") = MtpUser(id = id, username = username)

  @Nested
  @DisplayName("getPrisonsForUser (AUTH-051)")
  inner class GetPrisonsForUser {

    @Test
    fun `AUTH-051 returns all mapped prisons for a user`() {
      val user = makeUser()
      user.prisons = mutableSetOf(makePrison("LEI"), makePrison("MDI"))

      val result = prisonUserMappingService.getPrisonsForUser(user)

      assertThat(result).hasSize(2)
      assertThat(result.map { it.nomisId }).containsExactlyInAnyOrder("LEI", "MDI")
    }

    @Test
    fun `returns empty set when user has no prisons`() {
      val user = makeUser()

      val result = prisonUserMappingService.getPrisonsForUser(user)

      assertThat(result).isEmpty()
    }
  }

  @Nested
  @DisplayName("assignPrisons (AUTH-050)")
  inner class AssignPrisons {

    @Test
    fun `AUTH-050 assigns prisons to user`() {
      val user = makeUser()
      val prisons = setOf(makePrison("LEI"), makePrison("MDI"))
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      prisonUserMappingService.assignPrisons(user, prisons)

      assertThat(user.prisons).containsExactlyInAnyOrderElementsOf(prisons)
      verify(mtpUserRepository).save(user)
    }
  }

  @Nested
  @DisplayName("copyPrisonMapping (AUTH-052)")
  inner class CopyPrisonMapping {

    @Test
    fun `AUTH-052 copies prison assignments from source to target user`() {
      val source = makeUser(1L, "source")
      val target = makeUser(2L, "target")
      source.prisons = mutableSetOf(makePrison("LEI"), makePrison("MDI"))
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      prisonUserMappingService.copyPrisonMapping(source, target)

      assertThat(target.prisons).containsExactlyInAnyOrderElementsOf(source.prisons)
      verify(mtpUserRepository).save(target)
    }

    @Test
    fun `overrides any existing prison assignments on target`() {
      val source = makeUser(1L, "source")
      val target = makeUser(2L, "target")
      source.prisons = mutableSetOf(makePrison("LEI"))
      target.prisons = mutableSetOf(makePrison("BXI"))
      whenever(mtpUserRepository.save(any())).thenAnswer { it.arguments[0] }

      prisonUserMappingService.copyPrisonMapping(source, target)

      assertThat(target.prisons.map { it.nomisId }).containsExactly("LEI")
    }
  }
}
