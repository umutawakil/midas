package com.midas.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 6/28/22.
 *
 * TODO: Would be beneficial if this can be replaced with static calls. I could add beans
 * to it later if config from applicationProperties class is needed but at the moment
 * this isn't a service and should be made LoggingUtility
 *
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