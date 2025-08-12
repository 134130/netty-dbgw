package com.github.l34130.netty.dbgw.parser

import io.github.oshai.kotlinlogging.KotlinLogging
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.StatementVisitorAdapter
import net.sf.jsqlparser.statement.select.AllColumns
import net.sf.jsqlparser.statement.select.AllTableColumns
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter
import net.sf.jsqlparser.statement.select.SubSelect

private val logger = KotlinLogging.logger { }

private fun <T : Any> Iterable<T>.joinToPaddedString(): String = this.joinToString("\n") { it.toPaddedString() }

private fun Any.toPaddedString(padding: String = "  "): String = this.toString().split("\n").joinToString("\n") { "$padding$it" }

class ASqlVisitor {
    fun visit(sql: String) {
        val stmt: Statement = CCJSqlParserUtil.parse(sql)

        val stmtVisitor = AStatementVisitor()
        stmt.accept(stmtVisitor)
    }
}

class AStatementVisitor : StatementVisitorAdapter() {
    val selectVisitor = ASelectVisitor()

    override fun visit(select: Select) {
        select.selectBody.accept(selectVisitor)
    }
}

class ASelectVisitor : SelectVisitorAdapter() {
    val plainSelectVisitor = APlainSelectVisitor()

    override fun visit(plainSelect: PlainSelect) {
        plainSelect.accept(plainSelectVisitor)
    }
}

class APlainSelectVisitor : SelectVisitorAdapter() {
    val tableVisitor = ATableVisitor()
    val whereClauseVisitor = WhereClauseVisitor(tableVisitor)
    val columnVisitor = AColumnVisitor(tableVisitor)

    override fun visit(plainSelect: PlainSelect) {
        plainSelect.fromItem.accept(tableVisitor)
        plainSelect.joins?.forEach { join ->
            join.rightItem.accept(tableVisitor)
            join.onExpression?.accept(whereClauseVisitor)
        }
        plainSelect.where?.accept(whereClauseVisitor)
        plainSelect.selectItems.forEach { selectItem ->
            selectItem.accept(columnVisitor)
        }
    }
}

class ATableVisitor : FromItemVisitorAdapter() {
    val tableDefinitions: MutableList<TableDefinition> = mutableListOf()

    override fun visit(table: Table) {
        tableDefinitions +=
            PhysicalTableDefinition(
                catalogName = table.database.databaseName,
                schemaName = table.schemaName,
                tableName = table.name,
                alias = table.alias?.name,
            )
    }

    override fun visit(subSelect: SubSelect) {
        // If the FROM clause contains a subquery, we need to process it as well
        val subSelectVisitor = APlainSelectVisitor()
        subSelect.selectBody.accept(subSelectVisitor)

        // Add the subquery's table sources to the main table sources
        tableDefinitions +=
            DerivedTableDefinition(
                columns = subSelectVisitor.columnVisitor.columnRefs,
                references = subSelectVisitor.whereClauseVisitor.columnRefs,
                alias = checkNotNull(subSelect.alias?.name) { "Subquery must have an alias" },
            )
    }
}

class WhereClauseVisitor(
    val tableVisitor: ATableVisitor,
) : ExpressionVisitorAdapter() {
    val columnRefs = mutableListOf<ColumnRef>()

    override fun visit(column: Column) {
        val tableName = column.table?.name // In where clause, the table alias may not be present
        val columnName = column.columnName

        if (tableVisitor.tableDefinitions.size == 1) {
            // If there's only one table, we can assume the column belongs to that table
            val tableSource = tableVisitor.tableDefinitions.first()
            columnRefs += tableSource.getOriginalColumnSource(column.columnName)
            return
        }

        // If there are multiple tables, we need to find the correct table source
        val tableSource =
            tableVisitor.tableDefinitions.find { it.alias() == tableName }
                ?: error("Column '$columnName' refers to a table alias '$tableName' that does not exist in the FROM clause.")

        columnRefs +=
            ColumnRef(
                tableSource = tableSource,
                columnName = columnName,
            )
    }

    override fun visit(subSelect: SubSelect) {
        // If the WHERE clause contains a subquery, we need to process it as well
        val subSelectVisitor = APlainSelectVisitor()
        subSelect.selectBody.accept(subSelectVisitor)

        // TODO:
    }
}

class AColumnVisitor(
    private val tableVisitor: ATableVisitor,
) : ExpressionVisitorAdapter() {
    val columnRefs = mutableListOf<ColumnRef>()

    override fun visit(column: Column) {
        if (tableVisitor.tableDefinitions.size == 1) {
            // If there's only one table, we can assume the column belongs to that table
            val tableSource = tableVisitor.tableDefinitions.first()
            columnRefs += tableSource.getOriginalColumnSource(column.columnName)
            return
        }

        // If there are multiple tables, we need to find the correct table source
        val tableSource =
            tableVisitor.tableDefinitions.find { it.alias() == column.table?.name }
                ?: error(
                    "Column '${column.columnName}' refers to a table alias '${column.table?.name}' that does not exist in the FROM clause.",
                )

        columnRefs += tableSource.getOriginalColumnSource(column.columnName)
    }

    override fun visit(allColumns: AllColumns) {
        if (tableVisitor.tableDefinitions.size == 1) {
            // If there's only one table, we can assume all columns belong to that table
            val tableSource = tableVisitor.tableDefinitions.first()
            columnRefs +=
                ColumnRef(
                    tableSource = tableSource,
                    columnName = "*", // Represents all columns from the table
                )
            return
        }

        error(
            "AllColumns can only be used when there is exactly one table in the FROM clause. Found: ${tableVisitor.tableDefinitions.size} tables.",
        )
    }

    override fun visit(allTableColumns: AllTableColumns) {
        val tableName = allTableColumns.table.name
        val tableSource =
            tableVisitor.tableDefinitions.find { it.alias() == tableName }
                ?: error("Table '$tableName' does not exist in the FROM clause.")

        columnRefs +=
            ColumnRef(
                tableSource = tableSource,
                columnName = "*", // Represents all columns from the specified table
            )
    }
}

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
            is DerivedTableDefinition ->
                columns.find { it.columnName == columnName }
                    ?: error("Column '$columnName' does not exist in the derived table '$alias'.")
        }
}

data class PhysicalTableDefinition(
    val catalogName: String? = null,
    val schemaName: String? = null,
    val tableName: String,
    val alias: String? = null,
) : TableDefinition {
    override fun toString(): String =
        buildString {
            append("PhysicalTable(")
            append("catalogName='${catalogName ?: "null"}', ")
            append("schemaName='${schemaName ?: "null"}', ")
            append("tableName='$tableName', ")
            append("alias='${alias ?: "null"}'")
            append(")")
        }
}

data class DerivedTableDefinition(
    val columns: List<ColumnRef>,
    val references: List<ColumnRef> = emptyList(),
    val alias: String,
) : TableDefinition {
    override fun toString(): String =
        buildString {
            append("DerivedTable(")
            append("alias='$alias', ")

            if (columns.isEmpty()) {
                append("columns=[]")
            } else {
                appendLine("columns=[")
                append(columns.joinToPaddedString())
                appendLine("], ")
            }

            if (references.isEmpty()) {
                append("references=[]")
            } else {
                appendLine("references=[")
                append(references.joinToPaddedString())
                appendLine("]")
            }
            append(")")
        }
}

data class ColumnRef(
    val tableSource: TableDefinition,
    val columnName: String,
) {
    override fun toString(): String =
        buildString {
            append("ColumnSource(")
            append("tableSource='$tableSource', ")
            append("columnName='$columnName'")
            append(")")
        }
}

fun ColumnRef.fqn(): String =
    buildString {
        append(tableSource.fqn())
        append(".")
        append(columnName)
    }

fun TableDefinition.fqn(): String =
    buildString {
        if (this@fqn is PhysicalTableDefinition) {
            catalogName?.let { append("$it.") }
            schemaName?.let { append("$it.") }
        }
        append(name())
    }

fun ColumnRef.originColumns(): List<ColumnRef> =
    when (val tableSource = this.tableSource) {
        is PhysicalTableDefinition -> listOf(this)
        is DerivedTableDefinition -> tableSource.columns
    }

fun ColumnRef.originColumnsRecursive(): List<ColumnRef> =
    when (val tableSource = this.tableSource) {
        is PhysicalTableDefinition -> listOf(this)
        is DerivedTableDefinition -> tableSource.columns.flatMap { it.originColumnsRecursive() }
    }

fun ColumnRef.originReferences(): List<ColumnRef> =
    when (val tableSource = this.tableSource) {
        is PhysicalTableDefinition -> emptyList()
        is DerivedTableDefinition -> tableSource.references
    }

fun ColumnRef.originReferencesRecursive(): List<ColumnRef> =
    when (val tableSource = this.tableSource) {
        is PhysicalTableDefinition -> emptyList()
        is DerivedTableDefinition -> tableSource.references.flatMap { it.originReferencesRecursive() }
    }

fun printSourceTree(
    sb: StringBuilder,
    source: ColumnRef,
    label: String = "", // [1] 의존성 종류를 표시할 라벨 파라미터 추가
    indent: String = "  ",
    visited: MutableSet<Any> = mutableSetOf(),
) {
    // 순환 참조 방지를 위해 이미 방문한 노드는 출력하지 않음
    if (!visited.add(source)) return

    // [2] 출력문에 라벨을 추가하여 의존성 종류 표시
    sb.appendLine("$indent- $label${source.fqn()}")

    // [3] originColumns를 순회하며 '열' 라벨과 함께 재귀 호출
    source
        .originColumns()
        // 자기 자신을 참조하는 무한 루프 방지
        .takeIf { it.isNotEmpty() && it.first() != source }
        ?.forEach { origin ->
            printSourceTree(sb, origin, "[col] ", "$indent  ", visited)
        }

    // [4] originReferences를 순회하며 '참조' 라벨과 함께 재귀 호출
    source
        .originReferences()
        .takeIf { it.isNotEmpty() && it.first() != source }
        ?.forEach { reference ->
            printSourceTree(sb, reference, "[ref] ", "$indent  ", visited)
        }
}
