package com.github.l34130.netty.dbgw.policy.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef

internal inline fun <reified T> ObjectMapper.readValues(content: String): List<T> {
    val parser: JsonParser = this.factory.createParser(content)
    return this.readValues(parser, jacksonTypeRef<T>()).readAll()
}
