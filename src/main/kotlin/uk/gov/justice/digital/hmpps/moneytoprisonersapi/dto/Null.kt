package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Empty body type used by Python for endpoints with no request/response payload
 * (e.g. delete actions). Provided for spec parity with Python's `Null` schema.
 */
@Schema(name = "Null", description = "Empty body type")
class Null
