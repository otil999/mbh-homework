package hu.mbhbank.accounting

import hu.mbhbank.accounting.model.Account
import hu.mbhbank.accounting.model.Transaction
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface AccountRepository : CrudRepository<Account, Long> {
    /** Finds all active accounts */
    fun findByDeletedFalseAndCreatedTrue(): List<Account>

    /** Finds the not deleted account by its accountNumber */
    fun findByAccountNumberAndDeletedFalse(accountNumber: Long): Account?

    /** Finds the active (created and not deleted) account by its accountNumber */
    fun findByAccountNumberAndDeletedFalseAndCreatedTrue(accountNumber: Long): Account?
}


@Repository
interface TransactionRepository : CrudRepository<Transaction, UUID> {
    fun findByAccountOrderByTimestampDesc(account: Account): List<Transaction>
}
