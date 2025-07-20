package com.github.l34130.netty.dbgw.app

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import com.github.l34130.netty.dbgw.core.config.GatewayConfigLoader
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGateway
import com.github.l34130.netty.dbgw.protocol.postgres.PostgresGateway
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.OutputStreamWriter
import kotlin.system.exitProcess

class Args(
    parser: ArgParser,
) {
    val port by parser
        .storing("--port", help = "Port to listen gateway on") {
            toInt().also {
                require(it in (1..65535)) { "Port must be between 1 and 65535" }
            }
        }.default(null)

    val upstream by parser
        .storing("--upstream", help = "Upstream to connect to") {
            val parts = split(":")
            require(parts.size == 2) { "Upstream must be in the format host:port" }
            Pair(parts[0], parts[1].toInt()).also {
                require(it.second in (1..65535)) { "Upstream port must be between 1 and 65535" }
            }
        }.default(null)

    val config by parser
        .storing(
            "--config",
            help = "Path to the configuration file (YAML format)",
        ).default(null)
}

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    mainBody {
        val config =
            parse(args).let {
                val config = it.config
                if (config != null) {
                    if (it.port != null || it.upstream != null) {
                        throw IllegalArgumentException("Cannot specify --port or --upstream when using --config")
                    }
                    logger.info { "Loading configuration from file: $config" }
                    GatewayConfigLoader.loadFrom(File(config))
                } else {
                    val port = it.port
                    val upstream = it.upstream
                    if (port == null || upstream == null) {
                        // Try to load the default configuration
                        GatewayConfigLoader.loadDefault() ?: throw IllegalArgumentException(
                            "No configuration file provided and no default configuration found. " +
                                "Please specify --config or provide --port and --upstream.",
                        )
                    } else {
                        // Use the provided port and upstream
                        logger.info { "Using provided port: $port and upstream: $upstream" }
                        GatewayConfig(
                            listenPort = port,
                            upstreamHost = upstream.first,
                            upstreamPort = upstream.second,
                            upstreamDatabaseType = GatewayConfig.UpstreamDatabaseType.MYSQL,
                            restrictedSqlStatements = emptyList(),
                            authenticationOverride = null, // No authentication by default
                        )
                    }
                }
            }

        logger.info {
            "Starting gateway with configuration: $config"
        }

        val gateway =
            when (config.upstreamDatabaseType) {
                GatewayConfig.UpstreamDatabaseType.MYSQL -> {
                    MySqlGateway(config)
                }
                GatewayConfig.UpstreamDatabaseType.POSTGRESQL -> {
                    PostgresGateway(config)
                }
            }

        try {
            gateway.start()
        } finally {
            gateway.shutdown()
        }
    }
}

private fun parse(args: Array<String>): Args {
    val parser = ArgParser(args)
    return try {
        parser.parseInto(::Args)
    } catch (e: SystemExitException) {
        if (e is ShowHelpException) {
            throw e
        }

        val writer = OutputStreamWriter(if (e.returnCode == 0) System.out else System.err)
        writer.write("Error: ")
        e.printUserMessage(writer, null, 0)
        writer.write("\n")
        writer.flush()

        ArgParser(arrayOf("--help")).parseInto(::Args)
        exitProcess(e.returnCode)
    }
}
