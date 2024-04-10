package hu.mbhbank.accounting

import hu.mbhbank.accounting.model.*
import hu.mbhbank.accounting.service.AccountService
import hu.mbhbank.accounting.service.SecurityValidatorClient
import hu.mbhbank.accounting.service.TransactionService
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
import org.springframework.data.repository.findByIdOrNull
import java.sql.Timestamp
import java.util.*

@ExtendWith(MockKExtension::class)
@Tag("Unit")
class TransactionServiceUnitTests {
    private val createdAccount = Account(11223344L, "Dummy").apply { created = true }
    private val transactionRequest = TransactionCreateRequestDTO(
        createdAccount.accountNumber, TransactionType.DEPOSIT.value(), 11, System.currentTimeMillis()
    )
    private val transaction = Transaction(
        createdAccount,
        TransactionType.valueOf(transactionRequest.type),
        transactionRequest.amount,
        Timestamp(transactionRequest.timestamp)
    )
    private val accountService = mockk<AccountService>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val validator = mockk<Validator>()
    private val transactionService = TransactionService(accountService, transactionRepository, validator)

    // ==============
    // Positive tests
    // ==============

    @Test
    @DisplayName("""
        Given a transaction request for an active account, 
        When createTransaction method is called,
        Then it returns the created transaction and it is also persisted
    """)
    fun createTransaction() {
        // arrange
        every { accountService.getAccount(transactionRequest.accountNumber) } returns createdAccount
        every { validator.validate(any(Transaction::class)) } returns emptySet()
        every { transactionRepository.save(any(Transaction::class)) } returns transaction

        // act
        val createdTransaction = transactionService.createTransaction(transactionRequest)

        // assert
        assertEquals(transaction, createdTransaction)
        verify(exactly = 1) { transactionRepository.save(any(Transaction::class)) }
    }

    @Test
    @DisplayName("""
        Given a transaction of an active account, 
        When listTransaction method is called,
        Then it returns the transactions of the account
    """)
    fun listTransactions() {
        // arrange
        every { transactionRepository.findByAccountOrderByTimestampDesc(createdAccount) } returns listOf(transaction)

        // act
        val transactions = transactionService.listTransactions(createdAccount)

        // assert
        assertFalse(transactions.isEmpty())
        assertEquals(transaction, transactions[0])
    }

    @Test
    @DisplayName("""
        Given a transaction of an active account, 
        When getTransaction method is called with transaction id,
        Then it returns the transaction
    """)
    fun getTransaction() {
        // arrange
        every { transactionRepository.findByIdOrNull(transaction.id) } returns transaction

        // act
        val returnedTransaction = transactionService.getTransaction(transaction.id)

        // assert
        assertEquals(transaction, returnedTransaction)
    }

    @Test
    @DisplayName("""
        Given a future transaction, 
        When deleteTransaction method is called with transaction id,
        Then it deletes the transaction
    """)
    fun deleteTransaction() {
        // arrange
        transaction.timestamp = Timestamp(System.currentTimeMillis() + 1000)
        every { transactionRepository.findByIdOrNull(transaction.id) } returns transaction
        every { transactionRepository.delete(transaction) } answers {}

        // act
        transactionService.deleteTransaction(transaction.id)

        // assert
        verify(exactly = 1) { transactionRepository.delete(transaction) }
    }

    @Test
    @DisplayName("""
        Given an update request for a transaction, 
        When updateTransaction method is called,
        Then it returns the updated transaction and it is also persisted
    """)
    fun updateTransaction() {
        // arrange
        val transactionUpdateRequestDTO = TransactionUpdateRequestDTO(
            TransactionType.WITHDRAWAL.value(), 22, System.currentTimeMillis() + 1000L
        )
        every { validator.validate(any(Transaction::class)) } returns emptySet()
        every { transactionRepository.findByIdOrNull(any(UUID::class)) } returns transaction
        every { transactionRepository.save(any(Transaction::class)) } returns transaction

        // act
        val updatedTransaction = transactionService.updateTransaction(transaction.id, transactionUpdateRequestDTO)

        // assert
        assertEquals(transaction.id, updatedTransaction.id)
        assertEquals(TransactionType.valueOf(transactionUpdateRequestDTO.type), updatedTransaction.type)
        assertEquals(transactionUpdateRequestDTO.amount, updatedTransaction.amount)
        assertEquals(transactionUpdateRequestDTO.timestamp, updatedTransaction.timestamp.time)
        verify(exactly = 1) { transactionRepository.save(transaction) }
    }

    // ==============
    // Negative tests
    // ==============

    @Test
    @DisplayName("""
        Given a transaction request to perform on a not active account,
        When createTransaction method is called,
        Then UnprocessableTransactionException is thrown
    """)
    fun throwsExceptionWhenAccountIsNotActive() {
        // arrange
        every { accountService.getAccount(transactionRequest.accountNumber) } throws EntityNotFoundException()

        // act and assert

        // assert
        assertThrows(TransactionService.UnprocessableTransactionException::class.java) {
            transactionService.createTransaction(transactionRequest)
        }
    }

    @Test
    @DisplayName("Given a deleted account, When listTransaction method is called, Then Exception is thrown")
    fun listTransactionsOfDeletedAccount() {
        // arrange
        createdAccount.apply { deleted = true }

        // act and assert
        assertThrows(EntityNotFoundException::class.java) { transactionService.listTransactions(createdAccount) }
    }

    @Test
    @DisplayName("""
        Given an non existing transaction id, 
        When getTransaction method is called with that id,
        Then EntityNotFoundException is thrown
    """)
    fun getTransactionWithInvalidId() {
        // arrange
        val notExistingTransactionId = UUID.randomUUID()
        every { transactionRepository.findByIdOrNull(notExistingTransactionId) } returns null

        // act and assert
        assertThrows(EntityNotFoundException::class.java) { transactionService.getTransaction(notExistingTransactionId) }
    }

    @Test
    @DisplayName("""
        Given a completed transaction, 
        When deleteTransaction method is called with transaction id,
        Then it throws IllegalStateException
    """)
    fun deleteCompletedTransaction() {
        // arrange
        every { transactionRepository.findByIdOrNull(transaction.id) } returns transaction

        // act and assert
        assertThrows(IllegalStateException::class.java) { transactionService.deleteTransaction(transaction.id) }
    }
}
