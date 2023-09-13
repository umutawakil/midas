package com.midas

import com.midas.configuration.ApplicationProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class)
class MidasApplication

fun main(args: Array<String>) {
	val application = SpringApplication(MidasApplication::class.java)
	application.run(*args)
}
