package hu.mbhbank.accounting.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.sql.Timestamp
import java.util.*


enum class TransactionType {
    DEPOSIT { override fun value() = DEPOSIT.name },
    WITHDRAWAL { override fun value() = WITHDRAWAL.name };

    abstract fun value(): String
}


@Entity
class Transaction(
    @ManyToOne
    @JoinColumn(name = "account_number")
    val account: Account,

    @Enumerated(EnumType.STRING)
    var type: TransactionType,

    @get:Positive(message = "Transaction amount must be positive")
    var amount: Long,

    @get:Future(message = "Time of transaction can not be in the past")
    var timestamp: Timestamp
) {
    @Schema(description = "id of transaction", type = "String", example = "e289231c-95d9-4cd4-85ad-7fecaf63f740")
    @Id
    val id: UUID = UUID.randomUUID()

    override fun hashCode(): Int = Objects.hash(id)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transaction) return false
        return Objects.equals(id, other.id)
    }

    override fun toString() =
        "Transaction@${this.hashCode()}[id:$id,type:$type,amount:$amount,timestamp:$timestamp,account:$account]"

    fun toTransactionResponseDTO() = TransactionResponseDTO(id, type.value(), amount, timestamp.time)
}


@Schema(name = "TransactionUpdateRequest", description = "This model represents a transaction to be updated.")
data class TransactionUpdateRequestDTO(
    @Schema(description = "type of a transaction", example = "DEPOSIT")
    @get:Pattern(message = "Invalid transaction type", regexp = "^(DEPOSIT|WITHDRAWAL)$")
    val type: String,

    @Schema(description = "amount of transaction", example = "1234")
    @get:Positive(message = "Transaction amount must be positive")
    val amount: Long,

    @Schema(description = "timestamp of transaction in milliseconds", example = "1711787320000")
    @get:NotNull
    @get:Positive
    val timestamp: Long
)


@Schema(name = "TransactionCreateRequest", description = "This model represents a transaction to be created.")
class TransactionCreateRequestDTO(
    @Schema(description = "account number to perform transaction on", type = "Long", example = "1234567812345678")
    @get:NotNull
    @get:Positive
    @get:Min(1000000000000000L)
    @get:Max(9999999999999999L)
    val accountNumber: Long,

    @Schema(description = "type of a transaction", example = "DEPOSIT")
    @get:Pattern(message = "Invalid transaction type", regexp = "^(DEPOSIT|WITHDRAWAL)$")
    val type: String,

    @Schema(description = "amount of transaction", example = "1234")
    @get:Positive(message = "Transaction amount must be positive")
    val amount: Long,

    @Schema(description = "timestamp of transaction in milliseconds", example = "1711787320000")
    @get:NotNull
    @get:Positive
    val timestamp: Long
)


@Schema(name = "Transaction", description = "This model represents a transaction.")
data class TransactionResponseDTO(
    @Schema(description = "id of transaction", type = "String", example = "e289231c-95d9-4cd4-85ad-7fecaf63f740")
    val id: UUID,

    @Schema(description = "type of a transaction", example = "DEPOSIT")
    val type: String,

    @Schema(description = "amount of transaction", example = "1234")
    val amount: Long,

    @Schema(description = "timestamp of transaction in milliseconds", example = "1711787320000")
    val timestamp: Long
)
