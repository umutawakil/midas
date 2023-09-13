package com.midas.configuration

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 9/16/22.
 */
@Component
class ConnectionPoolInfo(
    @Autowired val hikariDataSource: HikariDataSource
) {
    @PostConstruct
    fun init() {
        /*
        Idle timeout: 600000
        maxLifetime: 1800000
        keepaliveTime: 0
        validationTimeout: 5000
        maximumPoolSize: 10
        connectionTimeout: 30000
        idleTimeout: 600000*/

        /** Default setting is 10!!!! WT!!! **/
        hikariDataSource.maximumPoolSize = 100

        println("\r\nHikari Connection Pool Configuration")
        println("Idle timeout: "+hikariDataSource.idleTimeout)
        println("maxLifetime: "+hikariDataSource.maxLifetime)
        println("keepaliveTime: "+hikariDataSource.keepaliveTime)
        println("validationTimeout: "+hikariDataSource.validationTimeout)
        println("maximumPoolSize: "+hikariDataSource.maximumPoolSize)
        println("connectionTimeout: "+hikariDataSource.connectionTimeout)
        println("idleTimeout: "+hikariDataSource.idleTimeout)
        println()
    }
}