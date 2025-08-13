package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.select.Select

data class ParseResult(
    val selectItems: Set<SelectItem>,
    val referencedColumns: Set<ColumnRef>,
//    val diagnostics: List<Diagnostic>
)

/**
 * SqlLineageAnalyzer parses a sql string and returns the lineage information
 * about the columns referenced in the sql.
 */
class SqlLineageAnalyzer {
    fun parse(sql: String): ParseResult {
        val stmt: Statement = CCJSqlParserUtil.parse(sql)
        val (selectItems, referencedColumns) =
            when (stmt) {
                is Select -> {
                    val selectVisitor = LineageSelectVisitor()
                    stmt.selectBody.accept(selectVisitor)
                    selectVisitor.selectItems to selectVisitor.referencedColumns
                }
                is Delete -> {
                    val deleteVisitor = LineageDeleteVisitor()

                    stmt.accept(deleteVisitor)

                    null to null
                    TODO()
                }
                else -> TODO("Unsupported statement type: ${stmt.javaClass.simpleName}")
            }

        return ParseResult(selectItems, referencedColumns)
    }
//    fun resolve(parsed: ParseResult, schemaProvider: SchemaProvider): LineageResult
    // convenience
//    fun analyze(sql: String, schemaProvider: SchemaProvider? = null): LineageResult
}
