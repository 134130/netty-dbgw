package com.github.l34130.netty.dbgw.test.postgres

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.policy.PolicyChangeListener
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.protocol.postgres.PostgresGateway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

@Testcontainers
abstract class PostgresIntegrationTestBase(
    image: String,
) {
    @Container
    private val postgresContainer =
        PostgreSQLContainer(image)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withExposedPorts(5432)

    private lateinit var gateway: PostgresGateway

    protected fun createDatabaseGatewayConfig(): DatabaseGatewayConfig =
        DatabaseGatewayConfig(
            listenPort = 0,
            upstreamHost = postgresContainer.host,
            upstreamPort = postgresContainer.getMappedPort(5432),
            upstreamDatabaseType = DatabaseGatewayConfig.UpstreamDatabaseType.POSTGRESQL,
            authenticationOverride = null,
        )

    @BeforeEach
    fun setup() {
        gateway =
            PostgresGateway(
                config = createDatabaseGatewayConfig(),
                policyConfigurationLoader =
                    object : PolicyConfigurationLoader {
                        override fun load(): List<PolicyDefinition> = listOf(PolicyDefinition.ALLOW_ALL)

                        override fun watchForChanges(listener: PolicyChangeListener): AutoCloseable =
                            AutoCloseable {
                                // No-op for this test
                            }
                    },
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
}
