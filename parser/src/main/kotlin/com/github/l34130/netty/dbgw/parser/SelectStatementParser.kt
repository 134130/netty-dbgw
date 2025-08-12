package com.github.l34130.netty.dbgw.parser

// class SelectStatementParser : ColumnTracker<Select> {
//    private val columnGraph: MutableMap<String, ColumnGraphNode> = mutableMapOf()
//    private val sourceMap: MutableMap<String, FromItem> = mutableMapOf()
//
//    override fun parse(stmt: Select): List<ColumnDefinition> {
// //        stmt.withItemsList?.forEach { withItem: WithItem ->
// //            sourceMap[withItem.name] = withItem.selectBody as FromItem // FIXME: as is invalid
// //            processSelectBody(withItem.selectBody, withItem.name)
// //        }
// //        processSelectBody(stmt.selectBody, null)
// //
// //        return columnGraph.values.map { node: ColumnGraphNode ->
// //            val origins = node.sources.flatMap { flatten(it) }
// //            ColumnDefinition(
// //                table = null, // Table name is not available in this context
// //                orgTables = origins.map { it.first },
// //                column = node.outputName,
// //                orgColumns = origins.map { it.second },
// //                columnType = SqlType.OTHER, // FIXME: Determine the actual type if possible
// //            )
// //        }
//        return emptyList()
//    }
//
//    private fun processSelectBody(
//        body: SelectBody,
//        parentAlias: String?,
//    ) {
//        when (body) {
//            is PlainSelect -> processPlainSelect(body, parentAlias)
//            else -> throw NotImplementedError("Unsupported SelectBody type: ${body.javaClass.simpleName}")
//        }
//    }
//
//    private fun processPlainSelect(
//        ps: PlainSelect,
//        parentAlias: String?,
//    ) {
//        registerFrom(ps.fromItem)
//        ps.joins?.forEach { registerFrom(it.rightItem) }
//        ps.selectItems.forEach { si: SelectItem ->
// //            if (si is SelectExpressionItem) {
// //                val outputName = si.alias?.name ?: si.expression.toString()
// //                val sources = collectSources(si.expression)
// //                val key = "${parentAlias ?: ""}.$outputName"
// //                columnGraph[key] = ColumnGraphNode(outputName, sources)
// //            }
//        }
//    }
//
//    private fun registerFrom(item: FromItem) {
//        val alias =
//            item.alias?.name
//                ?: (item as? Table)?.name
//                ?: return
//        sourceMap[alias] = item
//        if (item is SubSelect) {
//            processSelectBody(item.selectBody, alias)
//        }
//    }
//
//    private fun collectSources(expr: Expression): List<ColumnSource> {
//        val list = mutableListOf<ColumnSource>()
//        expr.accept(
//            object : ExpressionVisitorAdapter() {
//                override fun visit(column: Column) {
//                    list.add(
//                        ColumnSource(
//                            tableAlias = column.table?.name,
//                            columnName = column.columnName,
//                        ),
//                    )
//                }
//            },
//        )
//        return list
//    }
//
//    private fun flatten(src: ColumnSource): List<Pair<String, String>> {
//        val key = "${src.tableAlias}.${src.columnName}"
//        val node = columnGraph[key]
//        return emptyList()
// //        return if (node == null) {
// //            listOf(src.tableAlias.orEmpty() to src.columnName)
// //        } else {
// //            node.sources.flatMap { flatten(it) }
// //        }
//    }
// }
