package hu.mbhbank.accounting.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.security.SecureRandom
import java.util.*


@Schema(name = "Account", description = "This model represents an account.")
@Entity
class Account(
    @Schema(description = "id of the bank", type = "Long", example = "12345678")
    @Min(1000000000000000L)
    @Max(9999999999999999L)
    val bankId: Long,

    @Schema(description = "name of the account holder", type = "String", example = "John Doe")
    @Column(nullable = false)
    @NotBlank
    @NotEmpty
    val accountHolderName: String,
) {
    @Schema(description = "account number", type = "Long", example = "1234567812345678")
    @Id
    val accountNumber: Long = SecureRandom().nextLong(1000000000000000L, 10000000000000000L)

    var deleted: Boolean = false
    var created: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Account) return false
        return Objects.equals(accountNumber, other.accountNumber)
    }

    override fun hashCode(): Int = Objects.hash(accountNumber)

    override fun toString() =
        "Account@${this.hashCode()}[bankId:$bankId,number:$accountNumber,holder:$accountHolderName,created:$created,deleted:$deleted]"
}


@Schema(name = "AccountRequest", description = "This model represents an account to create.")
data class AccountRequestDTO(
    @Schema(description = "name of the account holder", type = "String", example = "John Doe")
    @get:NotEmpty
    @get:NotBlank
    val accountHolderName: String
)
