package com.github.l34130.netty.dbgw.test.mysql

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.policy.PolicyChangeListener
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGateway
import com.github.l34130.netty.dbgw.test.TestContainerUtils
import com.github.l34130.netty.dbgw.test.TestContainerUtils.databaseGatewayConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

@Testcontainers
abstract class MySqlIntegrationTestBase(
    private val image: String,
) {
    private lateinit var gateway: MySqlGateway

    protected fun createDatabaseGatewayConfig(): DatabaseGatewayConfig = TestContainerUtils.get(image).databaseGatewayConfig()

    @BeforeEach
    fun setup() {
        gateway =
            MySqlGateway(
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
                setProperty("sslMode", "DISABLED")
                setProperty("allowPublicKeyRetrieval", "true") // For 'caching_sha2_password' and 'sha256_password'
            }
        modifier(properties)

        val port: Int = properties.getProperty("port")?.toIntOrNull() ?: gateway.port()

        return DriverManager.getConnection(
            "jdbc:mysql://localhost:$port/testdb",
            properties,
        )
    }
}
