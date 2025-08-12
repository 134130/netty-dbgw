package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement

data class ParseResult(
    val columns: Set<ColumnRef>,
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

        val stmtVisitor = LineageStatementVisitor()
        stmt.accept(stmtVisitor)

        return ParseResult(
            stmtVisitor.columns,
            stmtVisitor.referencedColumns,
        )
    }
//    fun resolve(parsed: ParseResult, schemaProvider: SchemaProvider): LineageResult
    // convenience
//    fun analyze(sql: String, schemaProvider: SchemaProvider? = null): LineageResult
}
