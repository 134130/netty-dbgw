package com.github.l34130.netty.dbgw.parser

sealed interface TableDefinition {
    fun alias(): String? =
        when (this) {
            is PhysicalTableDefinition -> alias
            is DerivedTableDefinition -> alias
        }

    fun name(): String =
        when (this) {
            is PhysicalTableDefinition -> tableName
            is DerivedTableDefinition -> alias
        }

    fun getOriginalColumnSource(columnName: String): ColumnRef =
        when (this) {
            is PhysicalTableDefinition -> ColumnRef(this, columnName)
            is DerivedTableDefinition -> {
                val subqueryColumn =
                    this.columns.find { it.columnName == columnName }
                        ?: this.columns.find { it.columnName == "*" }
                        ?: error("Column '$columnName' does not exist in the derived table '$alias'.")

                val physicalColumnRef =
                    subqueryColumn.tableSource.getOriginalColumnSource(
                        if (subqueryColumn.columnName == "*") columnName else subqueryColumn.columnName,
                    )
                val physicalTable =
                    physicalColumnRef.tableSource as? PhysicalTableDefinition
                        ?: error("Recursive search for column origin did not resolve to a physical table.")

                ColumnRef(
                    tableSource = physicalTable.copy(alias = this.alias),
                    columnName = physicalColumnRef.columnName,
                )
            }
        }

    fun getAllReferences(): Set<ColumnRef> =
        when (this) {
            is PhysicalTableDefinition -> emptySet()
            is DerivedTableDefinition -> {
                val directReferences = this.references
                val nestedReferences = this.columns.flatMap { it.tableSource.getAllReferences() }
                directReferences + nestedReferences
            }
        }
}

data class PhysicalTableDefinition(
    val catalogName: String? = null,
    val schemaName: String? = null,
    val tableName: String,
    val alias: String? = null,
) : TableDefinition

data class DerivedTableDefinition(
    val columns: Set<ColumnRef>,
    val references: Set<ColumnRef> = emptySet(),
    val alias: String,
) : TableDefinition
