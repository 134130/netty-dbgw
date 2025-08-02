package com.github.l34130.netty.dbgw.test.mysql

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGateway
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

class MySqlIdleTimeoutIntegrationTest : MySqlIntegrationTestBase("mysql:8.0.28") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @Test
    fun `test idle timeout`() {
        val policyFile = File.createTempFile("policy", ".yaml").apply {
            writeText(
                """
                apiVersion: builtin/v1
                kind: DatabaseIdleTimeoutPolicy
                spec:
                  timeoutSeconds: 1
                """.trimIndent()
            )
        }
        val gateway = MySqlGateway(
            DatabaseGatewayConfig(
                listenPort = 3307,
                upstreamHost = mysqlContainer.host,
                upstreamPort = mysqlContainer.getMappedPort(3306),
                upstreamDatabaseType = DatabaseGatewayConfig.UpstreamDatabaseType.MYSQL,
                authenticationOverride = null,
                policyFile = policyFile.absolutePath
            )
        )
        gateway.start()
        val url = "jdbc:mysql://localhost:3307/testdb"
        val connection = createConnection()
        logger.info { "Connected to MySQL" }
        TimeUnit.SECONDS.sleep(2)
        assertFailsWith<Exception> {
            connection.createStatement().use { it.executeQuery("SELECT 1") }
        }
        gateway.stop()
    }
}
