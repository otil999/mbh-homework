package hu.mbhbank.accounting.controller

import hu.mbhbank.accounting.model.TransactionCreateRequestDTO
import hu.mbhbank.accounting.model.TransactionResponseDTO
import hu.mbhbank.accounting.model.TransactionUpdateRequestDTO
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
import java.util.*


@RestController
@RequestMapping(value = ["/api/v1/transactions"], produces = [APPLICATION_JSON_VALUE])
class TransactionController(private val transactionService: TransactionService) {
    @Operation(description = "Creates a new transaction for an account")
    @PostMapping
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Created"),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "422", description = "Transaction can not be performed on the account", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun createTransaction(@Valid @RequestBody transactionRequest: TransactionCreateRequestDTO): ResponseEntity<TransactionResponseDTO> {
        val transaction = transactionService.createTransaction(transactionRequest).toTransactionResponseDTO()
        return ResponseEntity.created(URI("/api/v1/transactions/${transaction.id}")).body(transaction)
    }

    @Operation(description = "Returns a transaction")
    @GetMapping("/{id}")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Ok"),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Transaction not found", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun getTransaction(@PathVariable @Valid id: UUID) =
        ResponseEntity.ok().body(transactionService.getTransaction(id).toTransactionResponseDTO())

    @Operation(description = "Deletes a transaction")
    @DeleteMapping("/{id}")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Transaction has been deleted successfully", content = [Content()]),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Transaction not found", content = [Content()]),
        ApiResponse(responseCode = "422", description = "Transaction can not be deleted", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun deleteTransaction(@PathVariable @Valid id: UUID): ResponseEntity<Void> {
        transactionService.deleteTransaction(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(description = "Updates a transaction")
    @PatchMapping("/{id}")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Transaction is updated"),
        ApiResponse(responseCode = "400", description = "Bad request", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Transaction not found", content = [Content()]),
        ApiResponse(responseCode = "422", description = "Transaction can not be updated", content = [Content()]),
        ApiResponse(responseCode = "500", description = "Server error", content = [Content()])
    ])
    fun updateTransaction(
        @PathVariable @Valid id: UUID,
        @RequestBody @Valid transactionRequest: TransactionUpdateRequestDTO
    ) = ResponseEntity.ok(transactionService.updateTransaction(id, transactionRequest).toTransactionResponseDTO())
}
