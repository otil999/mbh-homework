package hu.mbhbank.accounting.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull


data class ValidationRequest(
    val accountNumber: Long,
    val accountHolderName: String,
    val callbackUrl: String
)


data class ValidityCheckResult(
    @Schema(description = "account number", type = "Long", example = "1234567812345678")
    @get:NotNull
    @get:Min(1000000000000000L)
    @get:Max(9999999999999999L)
    val accountNumber: Long,

    @Schema(description = "indicates whether the security check passed", type = "Boolean", example = "true")
    @get:NotNull
    val isSecurityCheckSuccess: Boolean
)
