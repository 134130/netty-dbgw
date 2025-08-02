package com.github.l34130.netty.dbgw.test.mysql

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.policy.DatabasePolicyChain
import com.github.l34130.netty.dbgw.core.policy.PolicyEngine
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
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

    protected fun createDatabaseGatewayConfig(): DatabaseGatewayConfig =
        DatabaseGatewayConfig(
            listenPort = 0,
            upstreamHost = mysqlContainer.host,
            upstreamPort = mysqlContainer.getMappedPort(3306),
            upstreamDatabaseType = DatabaseGatewayConfig.UpstreamDatabaseType.MYSQL,
            authenticationOverride = null,
        )

    @BeforeEach
    fun setup() {
        gateway =
            MySqlGateway(
                createDatabaseGatewayConfig().apply {
                    policyEngine =
                        PolicyEngine(
                            policyChain =
                                DatabasePolicyChain(
                                    policies =
                                        listOf(
                                            object : DatabasePolicyInterceptor {
                                                override fun onAuthentication(
                                                    ctx: DatabaseContext,
                                                    evt: DatabaseAuthenticationEvent,
                                                ): PolicyDecision = PolicyDecision.Allow()

                                                override fun onQuery(ctx: DatabaseQueryContext): PolicyDecision = PolicyDecision.Allow()
                                            },
                                        ),
                                ),
                        )
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
