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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto.UpdatePrisonRequest
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.Credit
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.CreditResolution
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.PrisonPrison
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.CreditRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories.PrisonRepository
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("UpdatePrisonService")
class UpdatePrisonServiceTest {

  @Mock
  private lateinit var creditRepository: CreditRepository

  @Mock
  private lateinit var prisonRepository: PrisonRepository

  @InjectMocks
  private lateinit var updatePrisonService: UpdatePrisonService

  private fun makePrison(nomisId: String) = PrisonPrison().apply { this.nomisId = nomisId }

  private fun createCredit(
    id: Long = 1L,
    prisonerNumber: String? = "A1234BC",
    prison: PrisonPrison? = null,
    resolution: CreditResolution = CreditResolution.PENDING,
  ): Credit = Credit().apply {
    this.id = id
    amount = 1000L
    this.prisonerNumber = prisonerNumber
    prisonerName = "John Smith"
    this.prison = prison
    this.resolution = resolution.value
  }

  @Nested
  @DisplayName("CRD-220: updatePrisons() sets prison on matching credits")
  inner class UpdatePrisonsSetPrison {

    @Test
    @DisplayName("CRD-220 - sets prison on credits matching prisoner number")
    fun `should set prison on credits with matching prisoner number`() {
      val credit = createCredit(prisonerNumber = "A1234BC", prison = null)
      val lei = makePrison("LEI")
      whenever(prisonRepository.findById("LEI")).thenReturn(Optional.of(lei))
      whenever(creditRepository.findByPrisonerNumberAndPrisonIsNull("A1234BC")).thenReturn(listOf(credit))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }

      updatePrisonService.updatePrisons(listOf(UpdatePrisonRequest(prisonerNumber = "A1234BC", prison = "LEI")))

      assertThat(credit.prison?.nomisId).isEqualTo("LEI")
      verify(creditRepository).save(credit)
    }

    @Test
    @DisplayName("CRD-220 - does nothing for empty list")
    fun `should do nothing for empty list`() {
      updatePrisonService.updatePrisons(emptyList())

      verify(creditRepository, never()).findByPrisonerNumberAndPrisonIsNull(any())
      verify(creditRepository, never()).save(any())
    }

    @Test
    @DisplayName("CRD-221 - only updates credits with no prison assigned")
    fun `should only update credits with no prison assigned`() {
      val mdi = makePrison("MDI")
      val creditWithPrison = createCredit(prisonerNumber = "A1234BC", prison = mdi)
      val lei = makePrison("LEI")
      whenever(prisonRepository.findById("LEI")).thenReturn(Optional.of(lei))
      whenever(creditRepository.findByPrisonerNumberAndPrisonIsNull("A1234BC")).thenReturn(emptyList())

      updatePrisonService.updatePrisons(listOf(UpdatePrisonRequest(prisonerNumber = "A1234BC", prison = "LEI")))

      assertThat(creditWithPrison.prison?.nomisId).isEqualTo("MDI")
      verify(creditRepository, never()).save(creditWithPrison)
    }

    @Test
    @DisplayName("CRD-220 - handles multiple pairs in one call")
    fun `should handle multiple prisoner-prison pairs`() {
      val credit1 = createCredit(id = 1L, prisonerNumber = "A1234BC", prison = null)
      val credit2 = createCredit(id = 2L, prisonerNumber = "B5678DE", prison = null)
      val lei = makePrison("LEI")
      val mdi = makePrison("MDI")
      whenever(prisonRepository.findById("LEI")).thenReturn(Optional.of(lei))
      whenever(prisonRepository.findById("MDI")).thenReturn(Optional.of(mdi))
      whenever(creditRepository.findByPrisonerNumberAndPrisonIsNull("A1234BC")).thenReturn(listOf(credit1))
      whenever(creditRepository.findByPrisonerNumberAndPrisonIsNull("B5678DE")).thenReturn(listOf(credit2))
      whenever(creditRepository.save(any())).thenAnswer { it.arguments[0] }

      updatePrisonService.updatePrisons(
        listOf(
          UpdatePrisonRequest(prisonerNumber = "A1234BC", prison = "LEI"),
          UpdatePrisonRequest(prisonerNumber = "B5678DE", prison = "MDI"),
        ),
      )

      assertThat(credit1.prison?.nomisId).isEqualTo("LEI")
      assertThat(credit2.prison?.nomisId).isEqualTo("MDI")
    }
  }
}
