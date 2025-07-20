package com.github.l34130.netty.dbgw.test.mysql

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGateway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

@Testcontainers
abstract class MySqlIntegrationTestBase(
    image: String,
) {
    @Container
    private val mysqlContainer =
        MySQLContainer(image)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withExposedPorts(3306)

    private lateinit var gateway: MySqlGateway
    private val gatewayPort: Int = 3306

    @BeforeEach
    fun setup() {
        gateway =
            MySqlGateway(
                GatewayConfig(
                    listenPort = gatewayPort,
                    upstreamHost = mysqlContainer.host,
                    upstreamPort = mysqlContainer.getMappedPort(3306),
                    upstreamDatabaseType = GatewayConfig.UpstreamDatabaseType.MYSQL,
                    restrictedSqlStatements = emptyList(),
                    authenticationOverride = null,
                ),
            )
        // Start the gateway before each test
        gateway.start()
    }

    @AfterEach
    fun tearDown() {
        // Stop the gateway after each test
        gateway.stop()
    }

    protected fun createConnection(modifier: (props: Properties) -> Unit = {}): Connection {
        val properties =
            Properties().apply {
                setProperty("user", "testuser")
                setProperty("password", "testpass")
                setProperty("sslMode", "DISABLED")
            }
        modifier(properties)
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/testdb",
            properties,
        )
    }
}
