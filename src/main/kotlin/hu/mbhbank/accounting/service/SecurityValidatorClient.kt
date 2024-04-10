package hu.mbhbank.accounting.service

import hu.mbhbank.accounting.model.ValidationRequest
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody


@FeignClient(name = "security-validator", url = "\${hu.mbhbank.security-validator.url}")
interface SecurityValidatorClient {
    @PostMapping(value = ["/background-security-check"])
    fun securityCheck(@RequestBody validationRequest: ValidationRequest)
}
