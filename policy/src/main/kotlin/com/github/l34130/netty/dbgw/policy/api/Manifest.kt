package com.github.l34130.netty.dbgw.policy.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.readValues
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.StringWriter

class Manifest {
    @JsonIgnore
    val group: String

    @JsonIgnore
    val version: String

    val apiVersion: String
        get() = "$group/$version"
    val kind: String
    val metadata: Map<String, String>
    val spec: Map<String, Any>

    constructor(
        apiVersion: String,
        kind: String,
        metadata: Map<String, String> = emptyMap(),
        spec: Map<String, Any> = emptyMap(),
    ) {
        val parts = apiVersion.split('/')
        require(parts.size == 2) {
            "Invalid apiVersion format: $apiVersion. Expected format is 'group/version'."
        }
        this.group = parts[0]
        this.version = parts[1]
        this.kind = kind
        this.metadata = metadata
        this.spec = spec
    }

    override fun toString(): String = "ResourceManifest(apiVersion='$apiVersion', kind='$kind', metadata=$metadata, spec=$spec)"
}

private val yamlMapper =
    ObjectMapper(
        YAMLFactory().apply {
            disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
        },
    ).apply {
        registerKotlinModule()
    }

object ResourceManifestReader {
    fun readFrom(content: String) {
        val parser = yamlMapper.factory.createParser(content)
        val manifests = yamlMapper.readValues<Manifest>(parser).readAll()
        println(manifests)
    }
}

object ResourceManifestEncoder {
    fun encodeYaml(manifest: Manifest): String = yamlMapper.writeValueAsString(manifest)

    fun encodeYaml(manifests: List<Manifest>): String {
        val sw = StringWriter()
        val writer = yamlMapper.writer()
        writer.writeValues(sw).use { sequenceWriter ->
            sequenceWriter.writeAll(manifests)
        }
        return sw.toString()
    }
}
