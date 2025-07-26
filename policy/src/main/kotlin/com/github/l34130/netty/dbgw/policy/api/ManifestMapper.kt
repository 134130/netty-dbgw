package com.github.l34130.netty.dbgw.policy.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.StringWriter

object ManifestMapper {
    private val yamlMapper =
        ObjectMapper(
            YAMLFactory().apply {
                disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
            },
        ).apply {
            registerKotlinModule()
        }

    fun readValues(content: String) = yamlMapper.readValues<Manifest>(content)

    fun writeValueAsString(manifest: Manifest): String = yamlMapper.writeValueAsString(manifest)

    fun writeValuesAsString(manifests: List<Manifest>): String {
        val sw = StringWriter()
        val writer = yamlMapper.writer()
        writer.writeValues(sw).use { sequenceWriter ->
            sequenceWriter.writeAll(manifests)
        }
        return sw.toString()
    }
}
