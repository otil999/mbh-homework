package hu.mbhbank.accounting

import hu.mbhbank.accounting.model.Account
import hu.mbhbank.accounting.model.Transaction
import hu.mbhbank.accounting.model.TransactionType
import hu.mbhbank.accounting.service.BalanceService
import hu.mbhbank.accounting.service.TransactionService
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.Timestamp


@ExtendWith(MockKExtension::class)
@Tag("Unit")
class BalanceServiceUnitTests {
    private val transactionService = mockk<TransactionService>()
    private val balanceService = BalanceService(transactionService)

    @Test
    @DisplayName("""
        Given an active account with two completed transactions and a future transaction, 
        When balance is queried
        Then the proper balance is returned (sum of completed transaction amounts) 
    """)
    fun activeAccountBalance() {
        // arrange
        val account = Account(12341234L, "dummy")
        val transaction1 = Transaction(
            account, TransactionType.DEPOSIT, 11, Timestamp(System.currentTimeMillis() - 20000)
        )
        val transaction2 = Transaction(
            account, TransactionType.WITHDRAWAL, 33, Timestamp(System.currentTimeMillis() - 10000)
        )
        val transaction3 = Transaction(
            account, TransactionType.DEPOSIT, 55, Timestamp(System.currentTimeMillis() + 50000)
        )
        every { transactionService.listTransactions(account) } returns listOf(transaction1, transaction2, transaction3)

        // act
        val balance = balanceService.getAccountBalance(account).balance

        // assert
        assertEquals(-22, balance)
    }
}
