package com.github.l34130.netty.dbgw.policy

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File
import java.io.InputStream

internal inline fun <reified T> ObjectMapper.readValues(content: String): List<T> {
    val parser: JsonParser = this.factory.createParser(content)
    return this.readValues(parser, jacksonTypeRef<T>()).readAll()
}

internal inline fun <reified T> ObjectMapper.readValues(inputStream: InputStream): List<T> {
    val parser: JsonParser = this.factory.createParser(inputStream)
    return this.readValues(parser, jacksonTypeRef<T>()).readAll()
}

internal inline fun <reified T> ObjectMapper.readValues(file: File): List<T> {
    val parser: JsonParser = this.factory.createParser(file)
    return this.readValues(parser, jacksonTypeRef<T>()).readAll()
}
