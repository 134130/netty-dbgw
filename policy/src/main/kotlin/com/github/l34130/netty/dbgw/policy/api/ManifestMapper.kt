package com.github.l34130.netty.dbgw.policy.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.l34130.netty.dbgw.policy.readValues
import java.io.File
import java.io.InputStream
import java.io.StringWriter

object ManifestMapper {
    val Default: ObjectMapper =
        ObjectMapper(
            YAMLFactory().apply {
                disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
            },
        ).apply {
            registerKotlinModule()
        }

    fun readValues(file: File) = Default.readValues<Manifest>(file)

    fun readValues(inputStream: InputStream) = Default.readValues<Manifest>(inputStream)

    fun readValues(content: String) = Default.readValues<Manifest>(content)

    fun writeValueAsString(manifest: Manifest): String = Default.writeValueAsString(manifest)

    fun writeValuesAsString(manifests: List<Manifest>): String {
        val sw = StringWriter()
        val writer = Default.writer()
        writer.writeValues(sw).use { sequenceWriter ->
            sequenceWriter.writeAll(manifests)
        }
        return sw.toString()
    }
}

inline fun <reified T> ManifestMapper.convertValue(from: JsonNode): T {
    return Default.convertValue(from)
}
