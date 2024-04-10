package hu.mbhbank.accounting.service

import hu.mbhbank.accounting.model.Account
import hu.mbhbank.accounting.model.Balance
import hu.mbhbank.accounting.model.TransactionType
import jakarta.persistence.EntityNotFoundException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.sql.Timestamp


@Service
class BalanceService(private val transactionService: TransactionService) {
    private val logger = KotlinLogging.logger {}

    fun getAccountBalance(account: Account): Balance {
        if (account.deleted) throw EntityNotFoundException()

        val currentTimestamp = Timestamp(System.currentTimeMillis())
        return Balance(
            transactionService.listTransactions(account)
                .filter { transaction -> transaction.timestamp.before(currentTimestamp) }
                .also { logger.debug { "Filtered transactions: $it" } }
                .sumOf { transaction -> transaction.amount * (if (transaction.type == TransactionType.WITHDRAWAL) -1 else 1) }
        )
    }
}
