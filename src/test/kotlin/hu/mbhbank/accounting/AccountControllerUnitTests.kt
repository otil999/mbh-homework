package hu.mbhbank.accounting

import com.ninjasquad.springmockk.MockkBean
import hu.mbhbank.accounting.controller.AccountController
import hu.mbhbank.accounting.model.*
import hu.mbhbank.accounting.service.AccountService
import hu.mbhbank.accounting.service.BalanceService
import hu.mbhbank.accounting.service.TransactionService
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.every
import io.mockk.junit5.MockKExtension
import jakarta.persistence.EntityNotFoundException
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.sql.Timestamp


@WebMvcTest(value = [AccountController::class])
@AutoConfigureMockMvc(addFilters = false, printOnlyOnFailure = false)
@ExtendWith(MockKExtension::class)
@ExtendWith(SpringExtension::class)
@Tag("Unit")
class AccountControllerUnitTests {
    @Autowired private lateinit var mockMvc: MockMvc
    @MockkBean private lateinit var accountService: AccountService
    @MockkBean private lateinit var transactionService: TransactionService
    @MockkBean private lateinit var balanceService: BalanceService

    private val bankId = 12345678L
    private val accountHolder = "dummy"
    private val apiPath = "/api/v1/accounts"

    private val preparedAccount = Account(bankId, accountHolder)
    private val activeAccount = Account(bankId, accountHolder).apply { created = true }

    // ==============
    // positive tests
    // ==============

    @Test
    @DisplayName("""
        Given a request to prepare an account
        When POST endpoint is called
        Then 202 is returned with proper body content
    """)
    fun prepareAccount() {
        every { accountService.prepareAccount(accountHolder) } returns preparedAccount
        every { accountService.doSecurityCheck(preparedAccount) } answers {}

        mockMvc
            .perform(
                post("${apiPath}/prepare")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"accountHolderName":"${preparedAccount.accountHolderName}"}""")
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("bankId").value(bankId))
            .andExpect(jsonPath("accountHolderName").value(accountHolder))
            .andExpect(jsonPath("accountNumber").value(preparedAccount.accountNumber))
            .andExpect(jsonPath("deleted").value(false))
            .andExpect(jsonPath("created").value(false))
    }

    @Test
    @DisplayName("""
        Given a validity check result for an account holder with the permission to create the prepared account
        When POST endpoint is called
        Then 201 is returned with empty body and the created field is set
    """)
    fun createAccount() {
        every { accountService.createAccount(any(ValidityCheckResult::class)) } returns activeAccount

        mockMvc
            .perform(
                post("${apiPath}/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{
                        "accountNumber":"${activeAccount.accountNumber}",
                        "isSecurityCheckSuccess": true
                    }""".trimIndent())
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(content().string(""))
    }

    @Test
    @DisplayName("""
        Given two created accounts
        When GET endpoint is called to return accounts
        Then 200 is returned with the list of active accounts
    """)
    fun listAccounts() {
        val account2 = Account(bankId, "holder2").apply { created = true }
        every { accountService.listAccounts() } returns listOf(activeAccount, account2)

        mockMvc
            .perform(get(apiPath))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$", hasSize<Int>(2)))
            .andExpect(jsonPath("$[0].bankId").value(bankId))
            .andExpect(jsonPath("$[0].accountHolderName").value(accountHolder))
            .andExpect(jsonPath("$[0].accountNumber").value(activeAccount.accountNumber))
            .andExpect(jsonPath("$[0].deleted").value(false))
            .andExpect(jsonPath("$[0].created").value(true))
            .andExpect(jsonPath("$[1].bankId").value(bankId))
            .andExpect(jsonPath("$[1].accountHolderName").value(account2.accountHolderName))
            .andExpect(jsonPath("$[1].accountNumber").value(account2.accountNumber))
            .andExpect(jsonPath("$[1].deleted").value(false))
            .andExpect(jsonPath("$[1].created").value(true))
    }

    @Test
    @DisplayName("""
        Given a created account
        When GET endpoint is called with the account number
        Then 200 is returned with account information
    """)
    fun getActiveAccount() {
        every { accountService.getAccount(activeAccount.accountNumber) } returns activeAccount

        mockMvc
            .perform(get("$apiPath/${activeAccount.accountNumber}"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bankId").value(bankId))
            .andExpect(jsonPath("$.accountHolderName").value(accountHolder))
            .andExpect(jsonPath("$.accountNumber").value(activeAccount.accountNumber))
            .andExpect(jsonPath("$.created").value(true))
            .andExpect(jsonPath("$.deleted").value(false))
    }

    @Test
    @DisplayName("""
        Given a created account
        When DELETE endpoint is called
        Then 204 is returned
    """)
    fun inactivateAccount() {
        val account = Account(bankId, "holder1").apply { created = true }
        every { accountService.disableActiveAccount(account.accountNumber) } returns account

        mockMvc
            .perform(delete("$apiPath/${account.accountNumber}"))
            .andDo(print())
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
    }

    @Test
    @DisplayName("""
        Given a created account
        When GET endpoint is called to query the account balance
        Then 200 is returned with the balance
    """)
    fun getBalance() {
        val balance = 1234L
        every { accountService.getAccount(activeAccount.accountNumber) } returns activeAccount
        every { balanceService.getAccountBalance(activeAccount) } returns Balance(balance)

        mockMvc
            .perform(get("$apiPath/${activeAccount.accountNumber}/balance"))
            .andDo(print())
            .andExpect(jsonPath("$.balance").value(balance))
    }

    @Test
    @DisplayName("""
        Given a created account with a transaction
        When GET endpoint is called to list the account transactions
        Then 200 is returned with the list of transactions
    """)
    fun getAccountTransactions() {
        val transaction = Transaction(activeAccount, TransactionType.DEPOSIT, 1234L, Timestamp(System.currentTimeMillis()))
        val transactionResponseDTO = transaction.toTransactionResponseDTO()
        every { accountService.getAccount(activeAccount.accountNumber) } returns activeAccount
        every { transactionService.listTransactions(activeAccount) } returns listOf(transaction)

        mockMvc
            .perform(get("$apiPath/${activeAccount.accountNumber}/transactions"))
            .andDo(print())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize<Int>(1)))
            .andExpect(jsonPath("$[0].id").value(transactionResponseDTO.id.toStr()))
            .andExpect(jsonPath("$[0].type").value(transactionResponseDTO.type.toStr()))
            .andExpect(jsonPath("$[0].amount").value(transactionResponseDTO.amount))
            .andExpect(jsonPath("$[0].timestamp").value(transactionResponseDTO.timestamp))
    }

    // ==============
    // negative tests
    // ==============

    @ParameterizedTest
    @ValueSource(strings = [
        """""",
        """{}""",
        """{"accountHolderName": null}""",
        """{"accountHolderName": ""}"""
    ])
    @DisplayName("Given an invalid account prepare request, When the POST endpoint is called, Then 400 is returned")
    fun prepareAccountWithInvalidRequest(requestBody: String) {
        mockMvc
            .perform(
                post("${apiPath}/prepare")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        """""",
        """{}""",
        """{"accountNumber": null, "isSecurityCheckSuccess": null}""",
        """{"accountNumber": "", "isSecurityCheckSuccess": ""}""",
        """{"accountNumber": 0, "isSecurityCheckSuccess": true}""",
        """{"accountNumber": -1, "isSecurityCheckSuccess": true}""",
    ])
    @DisplayName("Given an invalid account prepare request, When the POST endpoint is called, Then 400 is returned")
    fun createAccountWithInvalidRequest(requestBody: String) {
        mockMvc
            .perform(
                post("${apiPath}/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Given an failed security check response, When the POST endpoint is called, Then 403 is returned")
    fun createAccountWithFailedCheck() {
        every { accountService.createAccount(any(ValidityCheckResult::class)) } throws
                AccountService.InsecureAccountHolderException("accountHolder")
        mockMvc
            .perform(
                post("${apiPath}/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"accountNumber": 1234567812345678, "isSecurityCheckSuccess": false}""")
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("Given a security check response, When the POST endpoint is called with invalid account number, Then 404 is returned")
    fun createAccountWithInvalidAccountNumber() {
        every { accountService.createAccount(any(ValidityCheckResult::class)) } throws EntityNotFoundException()
        mockMvc
            .perform(
                post("${apiPath}/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"accountNumber": 1234567812345678, "isSecurityCheckSuccess": false}""")
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Given an invalid account number, When GET endpoint is called, Then 400 is returned")
    fun getAccountWithInvalidAccountNumber() {
        mockMvc
            .perform(get("${apiPath}/invalidAccountNumber").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Given a not active account number, When GET endpoint is called, Then 404 is returned")
    fun getAccountWithDeletedAccountNumber() {
        every { accountService.getAccount(any(Long::class)) } throws EntityNotFoundException()
        mockMvc
            .perform(get("${apiPath}/1234567812345678").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Given an invalid account number, When DELETE endpoint is called, Then 400 is returned")
    fun deleteAccountWithInvalidAccountNumber() {
        mockMvc
            .perform(delete("${apiPath}/invalidAccountNumber").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Given a non existing account number, When DELETE endpoint is called, Then 404 is returned")
    fun deleteAccountWithNonExistingAccountNumber() {
        every { accountService.disableActiveAccount(any(Long::class)) } throws EntityNotFoundException()
        mockMvc
            .perform(delete("${apiPath}/1234567812345678").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Given an invalid account number, When GET endpoint is called to get the balance, Then 400 is returned")
    fun getBalanceWithInvalidAccountNumber() {
        mockMvc
            .perform(get("${apiPath}/invalidAccountNumber/balance").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Given a not active account number, When GET endpoint is called to get the balance, Then 404 is returned")
    fun getBalanceWithInactiveAccountNumber() {
        every { accountService.getAccount(any(Long::class)) } throws EntityNotFoundException()
        mockMvc
            .perform(get("${apiPath}/1234567812345678/balance").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("Given an invalid account number, When GET endpoint is called to get the transactions, Then 400 is returned")
    fun getTransactionsWithInvalidAccountNumber() {
        mockMvc
            .perform(get("${apiPath}/invalidAccountNumber/transactions").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Given a not active account number, When GET endpoint is called to get the transactions, Then 404 is returned")
    fun getTransactionsWithInactiveAccountNumber() {
        every { accountService.getAccount(any(Long::class)) } throws EntityNotFoundException()
        mockMvc
            .perform(get("${apiPath}/1234567812345678/transactions").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isNotFound)
    }
}
