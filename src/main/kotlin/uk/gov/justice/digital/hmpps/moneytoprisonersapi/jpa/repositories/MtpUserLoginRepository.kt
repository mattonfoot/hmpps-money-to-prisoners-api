package uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.repositories

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.moneytoprisonersapi.jpa.entities.MtpUserLogin

interface MtpUserLoginRepository : JpaRepository<MtpUserLogin, Long>
