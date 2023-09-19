package com.midas.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class LoggingService {
    var logger: Logger = LoggerFactory.getLogger(LoggingService::class.java)
    fun log(x: String) {
        logger.info(x)
    }

    fun error(x: String) {
        logger.error(x)
    }

    fun error(exception: Exception) {
        exception.printStackTrace()
        logger.error(exception.message, exception)
    }

    fun securityLog(message: String) {
        logger.error(message)
    }
}