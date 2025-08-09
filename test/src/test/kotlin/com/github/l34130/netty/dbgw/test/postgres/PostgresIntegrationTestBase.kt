package com.github.l34130.netty.dbgw.test.postgres

import com.github.l34130.netty.dbgw.core.AbstractGateway
import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.protocol.postgres.PostgresGateway
import com.github.l34130.netty.dbgw.test.ALLOW_ALL
import com.github.l34130.netty.dbgw.test.TestContainerUtils
import com.github.l34130.netty.dbgw.test.TestContainerUtils.databaseGatewayConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

@Testcontainers
abstract class PostgresIntegrationTestBase(
    private val image: String,
) {
    private lateinit var gateway: PostgresGateway

    protected fun createDatabaseGatewayConfig(): DatabaseGatewayConfig = TestContainerUtils.get(image).databaseGatewayConfig()

    @BeforeEach
    fun setup() {
        gateway =
            PostgresGateway(
                config = createDatabaseGatewayConfig(),
                policyConfigurationLoader = PolicyConfigurationLoader.ALLOW_ALL,
            )
        // Start the gateway before each test
        gateway.start()
    }

    @AfterEach
    fun tearDown() {
        // Stop the gateway after each test
        gateway.shutdown()
    }

    protected fun createConnection(modifier: (props: Properties) -> Unit = {}): Connection {
        val properties =
            Properties().apply {
                setProperty("user", "testuser")
                setProperty("password", "testpass")
                setProperty("sslmode", "disable")
            }
        modifier(properties)

        val port: Int = properties.getProperty("port")?.toIntOrNull() ?: gateway.port()

        return DriverManager.getConnection(
            "jdbc:postgresql://localhost:$port/testdb",
            properties,
        )
    }

    protected fun AbstractGateway.createConnection(modifier: (props: Properties) -> Unit = {}): Connection =
        this@PostgresIntegrationTestBase.createConnection {
            it.setProperty("port", this.port().toString())
            modifier(it)
        }
}
