package com.github.l34130.netty.dbgw.test.postgres

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.policy.DatabasePolicyChain
import com.github.l34130.netty.dbgw.core.policy.PolicyEngine
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
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
    private val gatewayPort: Int = 5432

    @BeforeEach
    fun setup() {
        gateway =
            PostgresGateway(
                DatabaseGatewayConfig(
                    listenPort = gatewayPort,
                    upstreamHost = postgresContainer.host,
                    upstreamPort = postgresContainer.getMappedPort(5432),
                    upstreamDatabaseType = DatabaseGatewayConfig.UpstreamDatabaseType.POSTGRESQL,
                    authenticationOverride = null,
                ).apply {
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
                setProperty("sslmode", "disable")
            }
        modifier(properties)
        return DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/testdb",
            properties,
        )
    }
}
