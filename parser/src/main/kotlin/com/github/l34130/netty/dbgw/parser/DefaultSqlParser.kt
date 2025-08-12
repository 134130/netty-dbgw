package com.github.l34130.netty.dbgw.parser

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition
import net.sf.jsqlparser.parser.CCJSqlParserUtil

internal class DefaultSqlParser : SqlParser {
    override fun parse(sql: String): List<ColumnDefinition> {
        val stmt = CCJSqlParserUtil.parse(sql)

        return when (stmt) {
//            is Select -> SelectStatementParser().parse(stmt)
//            is Select -> SqlColumnTracker().parseAndTrackColumns(stmt)
//            is Select -> ColumnLineageExtractor().extractColumnDefinitions(stmt)
            else -> throw NotImplementedError("Unsupported SQL statement type: ${stmt.javaClass.simpleName}")
        }
    }
}
