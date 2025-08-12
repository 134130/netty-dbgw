package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.StatementVisitorAdapter
import net.sf.jsqlparser.statement.select.AllColumns
import net.sf.jsqlparser.statement.select.AllTableColumns
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter
import net.sf.jsqlparser.statement.select.SubSelect

class LineageStatementVisitor : StatementVisitorAdapter() {
    val selectVisitor = LineageSelectVisitor()

    override fun visit(select: Select) {
        select.selectBody.accept(selectVisitor)
    }
}

class LineageSelectVisitor : SelectVisitorAdapter() {
    val plainSelectVisitor = LineagePlainSelectVisitor()

    override fun visit(plainSelect: PlainSelect) {
        plainSelect.accept(plainSelectVisitor)
    }
}

class LineagePlainSelectVisitor : SelectVisitorAdapter() {
    val tableVisitor = TableSourceVisitor()
    val whereClauseVisitor = WhereClauseColumnVisitor(tableVisitor)
    val columnVisitor = SelectColumnVisitor(tableVisitor)
    val referencedColumns: List<ColumnRef>
        get() {
            val fromWhere = whereClauseVisitor.columnRefs
            val fromSubQueries = tableVisitor.tableDefinitions.flatMap { it.getAllReferences() }
            return fromWhere + fromSubQueries
        }

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

class TableSourceVisitor : FromItemVisitorAdapter() {
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
        val subSelectVisitor = LineagePlainSelectVisitor()
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

class WhereClauseColumnVisitor(
    val tableVisitor: TableSourceVisitor,
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
        val subSelectVisitor = LineagePlainSelectVisitor()
        subSelect.selectBody.accept(subSelectVisitor)

        // TODO:
    }
}

class SelectColumnVisitor(
    private val tableVisitor: TableSourceVisitor,
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
