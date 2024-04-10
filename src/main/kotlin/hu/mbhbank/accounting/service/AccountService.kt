package hu.mbhbank.accounting.service

import hu.mbhbank.accounting.AccountRepository
import hu.mbhbank.accounting.model.*
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service


@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val securityValidatorClient: SecurityValidatorClient,
    private val validator: Validator
) {
    private val logger = KotlinLogging.logger {}

    @Value("\${hu.mbhbank.bankId}")
    private val bankId: Long = 0L

    @Value("\${hu.mbhbank.security-validator.call-back-url}")
    private val validatorCallBackUrl: String = ""

    fun prepareAccount(accountHolderName: String) = saveAccount(validateAccount(Account(bankId, accountHolderName)))

    private fun saveAccount(account: Account) =
        accountRepository
            .save(account)
            .also { logger.debug { "Account $account is saved." } }

    private fun validateAccount(account: Account): Account {
        val violations = validator.validate(account)
        if (violations.isNotEmpty()) throw ConstraintViolationException(violations)
        return account
    }

    @Async
    fun doSecurityCheck(account: Account) {
        try {
            securityValidatorClient.securityCheck(
                ValidationRequest(account.accountNumber, account.accountHolderName, validatorCallBackUrl)
            )
        } catch (e: Exception) {
            logger.warn(e) { "Error when waiting response for security check of account $account." }
        }
    }

    fun createAccount(validityCheckResult: ValidityCheckResult): Account {
        val accountNumber = validityCheckResult.accountNumber
        val account = accountRepository.findByAccountNumberAndDeletedFalse(accountNumber)
            ?: throw EntityNotFoundException(entityNotFoundErrorMessage(accountNumber))
        if (!validityCheckResult.isSecurityCheckSuccess) {
            throw InsecureAccountHolderException(account.accountHolderName)
        }
        return saveAccount(account.apply { created = true })
    }

    fun getAccount(accountNumber: Long) =
        accountRepository.findByAccountNumberAndDeletedFalseAndCreatedTrue(accountNumber)
            ?: throw EntityNotFoundException(entityNotFoundErrorMessage(accountNumber))

    fun listAccounts() = accountRepository.findByDeletedFalseAndCreatedTrue()

    fun disableActiveAccount(accountNumber: Long) = saveAccount(getAccount(accountNumber).apply { deleted = true })

    private fun entityNotFoundErrorMessage(accountNumber: Long) = "Account $accountNumber not found."

    class InsecureAccountHolderException(val accountHolderName: String) : Exception()
}
