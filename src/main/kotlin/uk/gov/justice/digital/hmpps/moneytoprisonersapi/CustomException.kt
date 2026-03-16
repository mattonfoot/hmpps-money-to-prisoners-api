package uk.gov.justice.digital.hmpps.moneytoprisonersapi

import org.springframework.http.HttpStatus

class CustomException constructor(message: String, val status: HttpStatus) : Exception(message)
