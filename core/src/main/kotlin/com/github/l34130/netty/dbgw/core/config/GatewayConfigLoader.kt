package com.github.l34130.netty.dbgw.core.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

object GatewayConfigLoader {
    private val logger = KotlinLogging.logger { }
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    /**
     * Loads the gateway configuration from the default location.
     * Search the classpath, the project root directory.
     */
    fun loadDefault(): DatabaseGatewayConfig? {
        logger.debug { "Loading gateway configuration automatically..." }

        // Load from classpath
        GatewayConfigLoader::class.java
            .getResourceAsStream("/dbgw.yaml")
            ?.use { inputStream -> mapper.readValue<DatabaseGatewayConfig.Wrapper>(inputStream) }
            ?.let {
                logger.info { "Gateway configuration automatically loaded from classpath." }
                return it.gateway
            }

        // Load from project root directory
        File("dbgw.yaml")
            .takeIf { it.exists() }
            ?.let { file -> file to mapper.readValue<DatabaseGatewayConfig.Wrapper>(file) }
            ?.let { (file, it) ->
                logger.info { "Gateway configuration automatically loaded from '${file.absolutePath}'" }
                return it.gateway
            }

        logger.warn { "Gateway configuration not found in classpath or project root directory." }
        return null
    }

    fun loadFrom(file: File): DatabaseGatewayConfig {
        require(!file.exists()) {
            "File ${file.absolutePath} does not exist."
        }
        return mapper.readValue<DatabaseGatewayConfig.Wrapper>(file).gateway
    }
}
