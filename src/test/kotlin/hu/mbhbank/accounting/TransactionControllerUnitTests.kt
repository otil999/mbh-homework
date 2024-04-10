package hu.mbhbank.accounting

import com.ninjasquad.springmockk.MockkBean
import hu.mbhbank.accounting.controller.TransactionController
import hu.mbhbank.accounting.model.*
import hu.mbhbank.accounting.service.TransactionService
import io.mockk.every
import io.mockk.junit5.MockKExtension
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.sql.Timestamp
import java.util.*


@WebMvcTest(value = [TransactionController::class])
@AutoConfigureMockMvc(addFilters = false, printOnlyOnFailure = false)
@ExtendWith(MockKExtension::class)
@ExtendWith(SpringExtension::class)
@Tag("Unit")
class TransactionControllerUnitTests {
    @Autowired private lateinit var mockMvc: MockMvc
    @MockkBean private lateinit var transactionService: TransactionService

    private val transaction = Transaction(
        Account(12345678L, "Dummy"),
        TransactionType.DEPOSIT,
        11,
        Timestamp(System.currentTimeMillis() + 1000)
    )
    private val apiPath = "/api/v1/transactions"

    // ==============
    // positive tests
    // ==============

    @Test
    @DisplayName("""
        Given a request to create a transaction for an active account
        When POST endpoint is called
        Then 201 is returned with proper body content
    """)
    fun createTransaction() {
        every { transactionService.createTransaction(any(TransactionCreateRequestDTO::class)) } returns transaction

        mockMvc
            .perform(
                post(apiPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{
                        "accountNumber": ${transaction.account.accountNumber},
                        "type": "${transaction.type.value()}",
                        "amount": ${transaction.amount},
                        "timestamp": ${transaction.timestamp.time}
                    }""".trimIndent())
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("id").value(transaction.id.toString()))
            .andExpect(jsonPath("type").value(transaction.type.value()))
            .andExpect(jsonPath("amount").value(transaction.amount))
            .andExpect(jsonPath("timestamp").value(transaction.timestamp.time))
    }

    @Test
    @DisplayName("""
        Given a stored transaction
        When GET endpoint is called with the id of the transaction
        Then 200 is returned with proper body content of the transaction
    """)
    fun getTransaction() {
        every { transactionService.getTransaction(transaction.id) } returns transaction

        mockMvc
            .perform(get("$apiPath/${transaction.id}").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("id").value(transaction.id.toString()))
            .andExpect(jsonPath("type").value(transaction.type.value()))
            .andExpect(jsonPath("amount").value(transaction.amount))
            .andExpect(jsonPath("timestamp").value(transaction.timestamp.time))
    }

    @Test
    @DisplayName("""
        Given a stored transaction
        When DELETE endpoint is called with the id of the transaction
        Then 204 is returned with empty body
    """)
    fun deleteTransaction() {
        every { transactionService.deleteTransaction(transaction.id) } answers {}

        mockMvc
            .perform(delete("$apiPath/${transaction.id}").characterEncoding("utf-8"))
            .andDo(print())
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("""
        Given a request to update a transaction for an active account
        When PATCH endpoint is called with the transaction id and with the update data in body
        Then 200 is returned with proper body content
    """)
    fun updateTransaction() {
        every { transactionService.updateTransaction(transaction.id, any(TransactionUpdateRequestDTO::class)) } returns
                transaction.apply {
                    type = TransactionType.WITHDRAWAL
                    amount = 22
                    timestamp = Timestamp(this.timestamp.time + 2000)
                }

        mockMvc
            .perform(
                patch("$apiPath/${transaction.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{
                        "type": "${transaction.type.value()}",
                        "amount": ${transaction.amount},
                        "timestamp": ${transaction.timestamp.time}
                    }""".trimIndent())
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("id").value(transaction.id.toString()))
            .andExpect(jsonPath("type").value(transaction.type.value()))
            .andExpect(jsonPath("amount").value(transaction.amount))
            .andExpect(jsonPath("timestamp").value(transaction.timestamp.time))
    }

    // ==============
    // negative tests
    // ==============

    @ParameterizedTest
    @ValueSource(strings = [
        """""",
        """{}""",
        """{"accountNumber": null, "type": null, "amount": null, "timestamp": null}""",
        """{"accountNumber": null, "type": "DEPOSIT", "amount": 11, "timestamp": 1711994280000}""",
        """{"accountNumber": 0, "type": "DEPOSIT", "amount": 11, "timestamp": 1711994280000}""",
        """{"accountNumber": -1, "type": "DEPOSIT", "amount": 11, "timestamp": 1711994280000}""",
        """{"accountNumber": 1234567812345678, "type": null, "amount": 11, "timestamp": 1711994280000}""",
        """{"accountNumber": 1234567812345678, "type": "xxx", "amount": 11, "timestamp": 1711994280000}""",
        """{"accountNumber": 1234567812345678, "type": "DEPOSIT", "amount": null, "timestamp": 1711994280000}""",
        """{"accountNumber": 1234567812345678, "type": "DEPOSIT", "amount": 0, "timestamp": 1711994280000}""",
        """{"accountNumber": 1234567812345678, "type": "DEPOSIT", "amount": -1, "timestamp": 1711994280000}""",
        """{"accountNumber": 1234567812345678, "type": "DEPOSIT", "amount": 1, "timestamp": null}""",
        """{"accountNumber": 1234567812345678, "type": "DEPOSIT", "amount": 1, "timestamp": -1}"""
    ])
    @DisplayName("Given an invalid request to create transaction, When the POST endpoint is called, Then 400 is returned")
    fun createTransactionWithInvalidRequest(requestBody: String) {
        mockMvc
            .perform(
                post(apiPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Given an invalid uuid to get a transaction, When the GET endpoint is called, Then 400 is returned")
    fun getTransactionWithInvalidRequest() {
        mockMvc
            .perform(
                get("$apiPath/not-valid-uuid")
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Given an invalid uuid to delete a transaction, When the DELETE endpoint is called, Then 400 is returned")
    fun deleteTransactionWithInvalidRequest() {
        mockMvc
            .perform(
                get("$apiPath/not-valid-uuid")
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        """""",
        """{}""",
        """{"type": null, "amount": null, "timestamp": null}""",
        """{"type": null, "amount": 11, "timestamp": 1711994280000}""",
        """{"type": "xxx", "amount": 11, "timestamp": 1711994280000}""",
        """{"type": "DEPOSIT", "amount": null, "timestamp": 1711994280000}""",
        """{"type": "DEPOSIT", "amount": 0, "timestamp": 1711994280000}""",
        """{"type": "DEPOSIT", "amount": -1, "timestamp": 1711994280000}""",
        """{"type": "DEPOSIT", "amount": 1, "timestamp": null}""",
        """{"type": "DEPOSIT", "amount": 1, "timestamp": -1}"""
    ])
    @DisplayName("Given an invalid request to update transaction, When the PATCH endpoint is called, Then 400 is returned")
    fun updateTransactionWithInvalidRequest(requestBody: String) {
        mockMvc
            .perform(
                patch("$apiPath/${UUID.randomUUID()}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Given a valid request to update transaction, When the PATCH endpoint is called with invalid id, Then 400 is returned")
    fun updateTransactionWithInvalidId() {
        mockMvc
            .perform(
                patch("$apiPath/invalid-uuid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"type": "DEPOSIT", "amount": 1, "timestamp": 1711994280000}""")
                    .characterEncoding("utf-8")
            )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }
}
