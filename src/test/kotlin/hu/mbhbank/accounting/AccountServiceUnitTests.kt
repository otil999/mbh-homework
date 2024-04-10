package hu.mbhbank.accounting

import hu.mbhbank.accounting.model.Account
import hu.mbhbank.accounting.model.ValidationRequest
import hu.mbhbank.accounting.model.ValidityCheckResult
import hu.mbhbank.accounting.service.AccountService
import hu.mbhbank.accounting.service.SecurityValidatorClient
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@Tag("Unit")
class AccountServiceUnitTests {
    private val bankId = 11223344L
    private val accountHolder = "Dummy"
    private val preparedAccount = Account(bankId, accountHolder)
    private val createdAccount = Account(bankId, accountHolder).apply { created = true }
    private val accountRepository = mockk<AccountRepository>()
    private val securityValidatorClient = mockk<SecurityValidatorClient>()
    private val validator = mockk<Validator>()
    private val service = AccountService(accountRepository, securityValidatorClient, validator)

    // ==============
    // Positive tests
    // ==============

    @Test
    @DisplayName("""
        Given a holder name to create an account for, 
        When prepare is called,
        Then account is prepared
    """)
    fun prepareAccount() {
        every { validator.validate(any(Account::class)) } returns emptySet()
        every { accountRepository.save(any(Account::class)) } returns preparedAccount

        // act
        val savedAccount = service.prepareAccount(preparedAccount.accountHolderName)

        // assert
        assertEquals(preparedAccount, savedAccount)
    }

    @Test
    @DisplayName("""
        Given an account to perform a security check on, 
        When security check is called,
        Then external security validation is invoked
    """)
    fun securityCheck() {
        // arrange
        val validationRequest = ValidationRequest(preparedAccount.accountNumber, preparedAccount.accountHolderName, "")
        every { securityValidatorClient.securityCheck(validationRequest) } answers {}

        // act
        service.doSecurityCheck(preparedAccount)

        // assert
        verify(exactly = 1) { securityValidatorClient.securityCheck(validationRequest) }
    }

    @Test
    @DisplayName("""
        Given a successful security validation to create the account,
        When save is called in the service,
        Then account is created and the entity is returned
    """)
    fun createAccount() {
        // arrange
        every { accountRepository.findByAccountNumberAndDeletedFalse(preparedAccount.accountNumber) } returns preparedAccount
        every { accountRepository.save(any()) } returns preparedAccount

        // act
        val savedAccount = service.createAccount(ValidityCheckResult(preparedAccount.accountNumber, true))

        // assert
        assertEquals(preparedAccount, savedAccount)
        assertTrue(savedAccount.created)
    }

    @Test
    @DisplayName("""
        Given an active account,
        When list is called in the service,
        Then it returns the list containing the account
    """)
    fun listAccounts() {
        // arrange
        every { accountRepository.findByDeletedFalseAndCreatedTrue() } returns listOf(createdAccount)

        // act
        val accounts = service.listAccounts()

        // assert
        assertEquals(1, accounts.size)
        assertEquals(createdAccount, accounts.first())
    }

    @Test
    @DisplayName("""
        Given a created account to delete,
        When delete is called in the service,
        Then it sets deleted flag to true and persists the entity without deletion
    """)
    fun inactivateAccount() {
        // arrange
        every { accountRepository.findByAccountNumberAndDeletedFalseAndCreatedTrue(createdAccount.accountNumber) } returns createdAccount
        every { accountRepository.save(any()) } returns createdAccount

        // act
        service.disableActiveAccount(createdAccount.accountNumber)

        // assert
        assertTrue(createdAccount.deleted)
        verify(exactly = 1) { accountRepository.save(createdAccount) }
        verify(exactly = 0) { accountRepository.delete(createdAccount) }
    }

    // ==============
    // Negative tests
    // ==============

    @Test
    @DisplayName("""
        Given a failed security validation to create the account,
        When save is called in the service,
        Then account is not saved and exception is thrown
    """)
    fun whenSecurityCheckedFailedAccountNotCreated() {
        // arrange
        every { accountRepository.findByAccountNumberAndDeletedFalse(preparedAccount.accountNumber) } returns preparedAccount

        // act and assert
        assertThrows(AccountService.InsecureAccountHolderException::class.java) {
            service.createAccount(ValidityCheckResult(preparedAccount.accountNumber, false))
        }
        verify(exactly = 0) { accountRepository.save(preparedAccount) }
        assertFalse(preparedAccount.created)
    }

    @Test
    @DisplayName("""
        Given an existing but deleted account,
        When getAccount() method is called with the account number,
        Then it throws exception
    """)
    fun gettingDeletedAccountThrowsException() {
        // arrange
        val inactiveAccount = Account(bankId, accountHolder).apply { deleted = true }
        every { accountRepository.findByAccountNumberAndDeletedFalseAndCreatedTrue(inactiveAccount.accountNumber) } returns null

        // act and assert
        assertThrows(EntityNotFoundException::class.java) { service.getAccount(inactiveAccount.accountNumber) }
    }

    @Test
    @DisplayName("""
        Given a successfully returned security validation on non-existing account number
        When creating the account
        Then exception is thrown
    """)
    fun createAccountOnNonExistingAccountThrowsException() {
        // arrange
        every { accountRepository.findByAccountNumberAndDeletedFalse(preparedAccount.accountNumber) } returns null

        // act and assert
        assertThrows(EntityNotFoundException::class.java) {
            service.createAccount(ValidityCheckResult(preparedAccount.accountNumber, true))
        }
    }
}
