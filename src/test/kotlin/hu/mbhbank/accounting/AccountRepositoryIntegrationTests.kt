package hu.mbhbank.accounting

import hu.mbhbank.accounting.model.Account
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles


@ActiveProfiles("test")
@Tag("Integration")
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AccountRepositoryIntegrationTests {
    private val dummyBankId = 11223344L
    private val activeAccount1 = Account(dummyBankId,"ActiveDummyAccount1").apply { created = true }
    private val activeAccount2 = Account(dummyBankId,"ActiveDummyAccount2").apply { created = true }
    private val deletedAccount1 = Account(dummyBankId,"DeletedDummyAccount1").apply { deleted = true }
    private val notCreatedAccount1 = Account(dummyBankId,"NotCreatedDummyAccount1")

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setUp() {
        accountRepository.saveAll(listOf(activeAccount1, activeAccount2, deletedAccount1, notCreatedAccount1))
    }

    // ==============
    // positive tests
    // ==============

    @Test
    @DisplayName("""
        Given two active accounts and one prepared and one deleted in the repository among other accounts
        When invoking findByDeletedFalseAndCreatedTrue() method
        Then it returns only active accounts
    """)
    fun returnsListOfActiveAccounts() {
        // act
        val activeAccounts = accountRepository.findByDeletedFalseAndCreatedTrue()

        // assert
        assertEquals(2, activeAccounts.size)
        assertTrue(activeAccounts.containsAll(listOf(activeAccount1, activeAccount2)))
    }

    @Test
    @DisplayName("""
        Given two active accounts in the repository among other accounts
        When invoking findByAccountNumberAndDeletedFalse() method with the account number
        Then it returns only the active account identified by its account number
    """)
    fun returnsActiveAccount() {
        // act
        val account = accountRepository.findByAccountNumberAndDeletedFalse(activeAccount1.accountNumber)

        // assert
        assertNotNull(account)
        assertEquals(activeAccount1, account)
    }

    @Test
    @DisplayName("""
        Given a prepared but not created account in the repository among other accounts
        When invoking findByAccountNumberAndDeletedFalse() method with the account number
        Then it returns only the proper account identified by its account number
    """)
    fun returnsPreparedAccount() {
        // act
        val account = accountRepository.findByAccountNumberAndDeletedFalse(notCreatedAccount1.accountNumber)

        // assert
        assertNotNull(account)
        assertEquals(notCreatedAccount1, account)
    }

    @Test
    @DisplayName("""
        Given two active accounts in the repository among other accounts
        When invoking findByAccountNumberAndDeletedFalseAndCreatedTrue() method with the account number
        Then it returns only the active account identified by its account number
    """)
    fun returnsCreatedAccount() {
        // act
        val account = accountRepository.findByAccountNumberAndDeletedFalseAndCreatedTrue(activeAccount1.accountNumber)

        // assert
        assertNotNull(account)
        assertEquals(activeAccount1, account)
    }

    // ==============
    // negative tests
    // ==============

    @Test
    @DisplayName("""
        Given a not existing account number
        When trying to find a not deleted account by that account number
        Then it returns null
    """)
    fun returnsNullWhenNotDeletedAccountNumberNotExists() {
        // act and assert
        assertNull(accountRepository.findByAccountNumberAndDeletedFalse(Long.MAX_VALUE))
    }

    @Test
    @DisplayName("""
        Given a not existing account number
        When trying to find a created account by that account number
        Then it returns null
    """)
    fun returnsNullWhenNotCreatedAccountNumberNotExists() {
        // act and assert
        assertNull(accountRepository.findByAccountNumberAndDeletedFalseAndCreatedTrue(Long.MAX_VALUE))
    }

    @Test
    @DisplayName("""
        Given a deleted account
        When trying to find it by its account number
        Then it returns null
    """)
    fun returnsNullWhenFindingTheDeletedAccount() {
        // act and assert
        assertNull(accountRepository.findByAccountNumberAndDeletedFalse(deletedAccount1.accountNumber))
        assertNull(accountRepository.findByAccountNumberAndDeletedFalseAndCreatedTrue(deletedAccount1.accountNumber))
    }
}
