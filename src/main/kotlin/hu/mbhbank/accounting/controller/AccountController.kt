package hu.mbhbank.accounting.controller

import hu.mbhbank.accounting.model.Account
import hu.mbhbank.accounting.model.AccountRequestDTO
import hu.mbhbank.accounting.model.ValidityCheckResult
import hu.mbhbank.accounting.service.AccountService
import hu.mbhbank.accounting.service.BalanceService
import hu.mbhbank.accounting.service.TransactionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI


@RestController
@RequestMapping(value = ["/api/v1/accounts"], produces = [APPLICATION_JSON_VALUE])
class AccountController(
    private val accountService: AccountService,
    private val transactionService: TransactionService,
    private val balanceService: BalanceService
) {
    @Operation(description = "Prepares a new account")
    @PostMapping("/prepare")
    @ApiResponses(value = [
        ApiResponse(responseCode = "202", description = "Accepted"),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun prepareAccount(@Valid @RequestBody accountRequest: AccountRequestDTO): ResponseEntity<Account> {
        val account = accountService.prepareAccount(accountRequest.accountHolderName)
        accountService.doSecurityCheck(account)
        return ResponseEntity.accepted().body(account)
    }

    @Operation(description = "Creates the prepared account")
    @PostMapping("/create")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Account created successfully"),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "403", description = "Security validation failed, account cannot be created", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Account not found", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun createAccount(@Valid @RequestBody validityCheckResult: ValidityCheckResult): ResponseEntity<Void> {
        val account = accountService.createAccount(validityCheckResult)
        return ResponseEntity.created(URI("/api/v1/accounts/${account.accountNumber}")).build()
        // TODO: Caller side fails when the response body is not empty
//        val account = accountService.create(validityChekResult)
//        return ResponseEntity.created(URI("/api/v1/accounts/${account.accountNumber}")).body(account)
    }

    @Operation(description = "Returns the list of active accounts")
    @GetMapping
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Ok"),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun listAccounts(): ResponseEntity<List<Account>> = ResponseEntity.ok(accountService.listAccounts())

    @Operation(description = "Returns detailed information of the account")
    @GetMapping("/{accountNumber}")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Ok"),
        ApiResponse(responseCode = "400", description = "Bad request"),
        ApiResponse(responseCode = "404", description = "Account not found"),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun getAccount(@PathVariable @Valid accountNumber: Long): ResponseEntity<Account> =
        ResponseEntity.ok(accountService.getAccount(accountNumber))

    @Operation(description = "Deletes (inactivates) the given account")
    @DeleteMapping("/{accountNumber}")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Ok"),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Account not found", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun disableAccount(@PathVariable @Valid accountNumber: Long): ResponseEntity<Void> {
        accountService.disableActiveAccount(accountNumber)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Returns the balance of the given account")
    @GetMapping("/{accountNumber}/balance")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Ok"),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Account not found (deleted or not created yet)", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun getBalance(@PathVariable @Valid accountNumber: Long) =
        ResponseEntity.ok(balanceService.getAccountBalance(accountService.getAccount(accountNumber)))

    @Operation(description = "Returns the transactions of an active account")
    @GetMapping("/{accountNumber}/transactions")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Ok"),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Account not found (deleted or not created yet)", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun listTransactions(@PathVariable @Valid accountNumber: Long) =
        ResponseEntity.ok().body(
            transactionService
                .listTransactions(accountService.getAccount(accountNumber))
                .map { transaction -> transaction.toTransactionResponseDTO() }
                .toList()
        )
}
