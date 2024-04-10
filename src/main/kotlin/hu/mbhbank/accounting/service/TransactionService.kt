package hu.mbhbank.accounting.service

import hu.mbhbank.accounting.TransactionRepository
import hu.mbhbank.accounting.model.*
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*


@Service
class TransactionService(
    private val accountService: AccountService,
    private val transactionRepository: TransactionRepository,
    private val validator: Validator
) {
    private val logger = KotlinLogging.logger {}

    fun createTransaction(transactionRequest: TransactionCreateRequestDTO) =
        saveTransaction(
            validateTransaction(
                Transaction(
                    getActiveAccount(transactionRequest.accountNumber),
                    TransactionType.valueOf(transactionRequest.type),
                    transactionRequest.amount,
                    Timestamp(transactionRequest.timestamp)
                )
            )
        )

    private fun saveTransaction(transaction: Transaction) =
        transactionRepository
            .save(transaction)
            .also { logger.debug { "Transaction $transaction is saved." } }

    private fun validateTransaction(transaction: Transaction): Transaction {
        val violations = validator.validate(transaction)
        if (violations.isNotEmpty()) throw ConstraintViolationException(violations)
        return transaction
    }

    private fun getActiveAccount(accountNumber: Long) =
        try {
            accountService.getAccount(accountNumber)
        } catch (ex: EntityNotFoundException) {
            throw UnprocessableTransactionException("Account is not active")
        }

    fun listTransactions(account: Account): List<Transaction> {
        if (account.deleted) throw EntityNotFoundException()
        return transactionRepository.findByAccountOrderByTimestampDesc(account)
    }

    fun getTransaction(id: UUID) =
        transactionRepository.findByIdOrNull(id) ?: throw EntityNotFoundException("Transaction $id not found")

    fun deleteTransaction(id: UUID) {
        val transaction = getTransaction(id)
        if (transaction.timestamp.before(Timestamp(System.currentTimeMillis())))
            throw IllegalStateException("Completed transaction cannot be deleted")
        transactionRepository.delete(getTransaction(id))
    }

    fun updateTransaction(id: UUID, transactionRequest: TransactionUpdateRequestDTO) =
        saveTransaction(
            validateTransaction(
                getTransaction(id).apply {
                    amount = transactionRequest.amount
                    type = TransactionType.valueOf(transactionRequest.type)
                    timestamp = Timestamp(transactionRequest.timestamp)
                }
            )
        )

    class UnprocessableTransactionException(message: String) : Exception(message)
}
