package hu.mbhbank.accounting

import hu.mbhbank.accounting.model.Account
import hu.mbhbank.accounting.model.Transaction
import hu.mbhbank.accounting.model.TransactionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.sql.Timestamp


@ActiveProfiles("test")
@Tag("Integration")
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TransactionRepositoryIntegrationTests {
    private val dummyBankId = 12341234L

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    private val account1 = Account(dummyBankId, "DummyAccount1")
    private val account2 = Account(dummyBankId, "DummyAccount2")

    @BeforeEach
    fun setUp() {
        accountRepository.saveAll(listOf(account1, account2))
    }

    // ==============
    // Positive Tests
    // ==============

    @Test
    @DisplayName("""
        Given two accounts with transactions
        When querying the transactions of account1
        Then it returns only the list of transactions performed by account1 and the list is ordered by timestamp
    """)
    fun returnsListOfTransactionOfAnAccount() {
        // arrange
        val transaction1 =
            Transaction(account1, TransactionType.DEPOSIT, 1, Timestamp(System.currentTimeMillis() + 1000L))
        val transaction2 =
            Transaction(account1, TransactionType.DEPOSIT, 2, Timestamp(System.currentTimeMillis() + 2000L))
        val transaction3 =
            Transaction(account2, TransactionType.DEPOSIT, 1, Timestamp(System.currentTimeMillis() + 1000L))
        transactionRepository.saveAll(listOf(transaction1, transaction2, transaction3))

        // act
        val listOfAccount1Transactions = transactionRepository.findByAccountOrderByTimestampDesc(account1)

        // assert
        assertEquals(2, listOfAccount1Transactions.size)
        assertTrue(listOfAccount1Transactions.containsAll(listOf(transaction1, transaction2)))
        assertFalse(listOfAccount1Transactions.contains(transaction3))
        assertEquals(transaction2, listOfAccount1Transactions[0])
        assertEquals(transaction1, listOfAccount1Transactions[1])
    }

    // ==============
    // Negative Tests
    // ==============

    @Test
    @DisplayName("""
        Given an account1 without transactions
        When finding all transactions of account1
        Then it returns an empty list
    """)
    fun returnsEmptyListWhenNoTransactionsOfAccount() {
        // act
        val transactions = transactionRepository.findByAccountOrderByTimestampDesc(account1)

        // assert
        assertTrue(transactions.isEmpty())
    }
}
