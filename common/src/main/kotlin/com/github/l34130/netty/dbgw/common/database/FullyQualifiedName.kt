package com.github.l34130.netty.dbgw.common.database

data class FullyQualifiedName(
    val catalog: String,
    val schema: String? = null,
    val table: String? = null,
    val column: String? = null,
) {
    override fun toString(): String =
        buildString {
            append(catalog)
            schema?.let { append(".$it") }
            table?.let { append(".$it") }
            column?.let { append(".$it") }
        }
}
