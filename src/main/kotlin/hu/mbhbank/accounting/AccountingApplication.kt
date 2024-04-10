package hu.mbhbank.accounting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.scheduling.annotation.EnableAsync
import java.security.SecureRandom
import java.util.*


@SpringBootApplication
@EnableFeignClients
@EnableAsync
class AccountingApplication


fun main(args: Array<String>) {
	runApplication<AccountingApplication>(*args)
}
