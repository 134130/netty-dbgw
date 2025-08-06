package com.github.l34130.netty.dbgw.test

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer

object TestContainerUtils {
    private val containers = mutableMapOf<String, GenericContainer<*>>()

    fun get(image: String): GenericContainer<*> =
        containers.getOrPut(image) {
            when {
                image.startsWith("mysql:") ->
                    MySQLContainer(image)
                        .withUsername("testuser")
                        .withPassword("testpass")
                        .withDatabaseName("testdb")
                        .withExposedPorts(3306)
                        .apply { start() }
                image.startsWith("postgres:") ->
                    PostgreSQLContainer(image)
                        .withDatabaseName("testdb")
                        .withUsername("testuser")
                        .withPassword("testpass")
                        .withExposedPorts(5432)
                        .apply { start() }
                else -> throw IllegalArgumentException("Unsupported image: $image")
            }
        }

    fun GenericContainer<*>.databaseGatewayConfig(): DatabaseGatewayConfig =
        DatabaseGatewayConfig(
            listenPort = 0,
            upstreamHost = this.host,
            upstreamPort = this.getMappedPort(this.exposedPorts.first()),
            upstreamDatabaseType = DatabaseGatewayConfig.UpstreamDatabaseType.MYSQL,
            authenticationOverride = null,
        )
}
