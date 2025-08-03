package com.github.l34130.netty.dbgw.policy.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.l34130.netty.dbgw.policy.readValues
import java.io.File
import java.io.InputStream
import java.io.StringWriter

object ResourceManifestMapper {
    private val yamlMapper =
        ObjectMapper(
            YAMLFactory().apply {
                disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
            },
        ).apply {
            registerKotlinModule()
        }

    fun readValues(file: File) = yamlMapper.readValues<ResourceManifest>(file)

    fun readValues(inputStream: InputStream) = yamlMapper.readValues<ResourceManifest>(inputStream)

    fun readValues(content: String) = yamlMapper.readValues<ResourceManifest>(content)

    fun writeValueAsString(manifest: ResourceManifest): String = yamlMapper.writeValueAsString(manifest)

    fun writeValuesAsString(manifests: List<ResourceManifest>): String {
        val sw = StringWriter()
        val writer = yamlMapper.writer()
        writer.writeValues(sw).use { sequenceWriter ->
            sequenceWriter.writeAll(manifests)
        }
        return sw.toString()
    }
}
