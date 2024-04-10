package hu.mbhbank.accounting

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@OpenAPIDefinition(info = Info(
    title = "Accounting service",
    description = "Service to manage accounts and its transactions.",
    version = "0.0.1",
    contact = Contact(name = "info@mbhbank.hu")
))
class OpenApi3Configuration {
    @Bean
    fun internalApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("internal")
            .packagesToScan("hu.mbhbank.account.internal")
            .build()

    @Bean
    fun publicApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("public")
            .packagesToScan("hu.mbhbank.accounting")
            .build()

    @Bean
    fun allApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("all")
            .packagesToScan("hu.mbhbank.accounting")
            .build()
}
