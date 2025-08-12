package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement

data class ParseResult(
    val columns: List<ColumnRef>,
    val referencedColumns: List<ColumnRef>,
//    val diagnostics: List<Diagnostic>
)

class SqlLineageAnalyzer {
    fun parse(sql: String): ParseResult {
        val stmt: Statement = CCJSqlParserUtil.parse(sql)

        val stmtVisitor = AStatementVisitor()
        stmt.accept(stmtVisitor)

        return ParseResult(
            stmtVisitor.selectVisitor.plainSelectVisitor.columnVisitor.columnRefs,
            stmtVisitor.selectVisitor.plainSelectVisitor.whereClauseVisitor.columnRefs,
        )
    }
//    fun resolve(parsed: ParseResult, schemaProvider: SchemaProvider): LineageResult
    // convenience
//    fun analyze(sql: String, schemaProvider: SchemaProvider? = null): LineageResult
}
