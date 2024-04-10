package hu.mbhbank.accounting.controller

import hu.mbhbank.accounting.service.AccountService
import hu.mbhbank.accounting.service.TransactionService
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class GlobalRestExceptionHandler : ResponseEntityExceptionHandler() {
    data class ErrorDetails(
        val code: Int = 0,
        val messages: List<String> = emptyList()
    )

    enum class AppError {
        ENTITY_NOT_FOUND { override fun value() = 1 },
        INSECURE_ACCOUNT { override fun value() = 2 },
        UNPROCESSABLE_TRANSACTION { override fun value() = 3 },
        COMPLETED_TRANSACTION { override fun value() = 4 },
        CONSTRAINT_VIOLATION { override fun value() = 5 };

        abstract fun value(): Int
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> = ResponseEntity(
        ErrorDetails(
            HttpStatus.BAD_REQUEST.value(),
            ex.bindingResult.fieldErrors
                .filterNotNull()
                .map { m -> m.field + " " + m.defaultMessage }
                .toList()
        ),
        HttpStatus.BAD_REQUEST
    )

    @ExceptionHandler(value = [EntityNotFoundException::class])
    protected fun handleEntityNotFoundException(ex: EntityNotFoundException, request: WebRequest): ResponseEntity<Any> =
        ResponseEntity(ErrorDetails(AppError.ENTITY_NOT_FOUND.value(), listOf(ex.message ?: "")), HttpStatus.NOT_FOUND)

    @ExceptionHandler(value = [AccountService.InsecureAccountHolderException::class])
    protected fun handleInsecureAccountHolderException(ex: AccountService.InsecureAccountHolderException, request: WebRequest): ResponseEntity<Any>? =
        ResponseEntity(ErrorDetails(AppError.INSECURE_ACCOUNT.value(), listOf(ex.accountHolderName)), HttpStatus.FORBIDDEN)

    @ExceptionHandler(value = [TransactionService.UnprocessableTransactionException::class])
    protected fun handleUnprocessableTransactionException(ex: TransactionService.UnprocessableTransactionException, request: WebRequest): ResponseEntity<Any>? =
        ResponseEntity(ErrorDetails(AppError.UNPROCESSABLE_TRANSACTION.value(), listOf(ex.message ?: "")), HttpStatus.UNPROCESSABLE_ENTITY)

    @ExceptionHandler(value = [IllegalStateException::class])
    protected fun handleIllegalStateException(ex: IllegalStateException, request: WebRequest): ResponseEntity<Any>? =
        ResponseEntity(ErrorDetails(AppError.COMPLETED_TRANSACTION.value(), listOf(ex.message ?: "")), HttpStatus.UNPROCESSABLE_ENTITY)

    @ExceptionHandler(value = [ConstraintViolationException::class])
    protected fun handleConstraintViolationException(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<Any> =
        ResponseEntity(
            ErrorDetails(AppError.CONSTRAINT_VIOLATION.value(), ex.constraintViolations.map { violation -> violation.message }.toList()),
            HttpStatus.BAD_REQUEST
        )
}
