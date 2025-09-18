package com.pluxity.aiot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class AiotApplication

fun main(args: Array<String>) {
    runApplication<AiotApplication>(*args)
}
