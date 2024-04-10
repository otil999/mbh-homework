package hu.mbhbank.accounting.model

import io.swagger.v3.oas.annotations.media.Schema


data class Balance(
    @Schema(description = "current balance of the account", example = "1234")
    val balance: Long
)
