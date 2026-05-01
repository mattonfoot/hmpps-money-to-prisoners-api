package uk.gov.justice.digital.hmpps.moneytoprisonersapi.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Single item in the bulk transaction refund request.
 *
 * Mirrors Python's `UpdateRefundedTransaction` schema for serialiser-level compatibility.
 */
@Schema(name = "UpdateRefundedTransaction", description = "Mark a single transaction as refunded")
data class UpdateRefundedTransaction(
  @Schema(description = "Transaction ID", example = "42", required = true)
  val id: Long,
  @Schema(description = "Whether the transaction was refunded", example = "true", required = true)
  val refunded: Boolean,
)
