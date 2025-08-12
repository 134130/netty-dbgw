package com.github.l34130.netty.dbgw.parser

// class SqlColumnTracker {
//    private val tables = mutableMapOf<String, TableInfo>() // alias -> TableInfo
//    private val columnGraph = mutableListOf<ColumnGraphNode>()
//
//    fun parseAndTrackColumns(stmt: Statement): List<ColumnDefinition> {
//        val graph = buildColumnGraph(stmt)
//        return flattenGraph(graph)
//    }
//
//    fun buildColumnGraph(stmt: Statement): List<ColumnGraphNode> =
//        when (stmt) {
//            is Select -> processSelect(stmt)
//            else -> throw NotImplementedError("Unsupported SQL statement type: ${stmt.javaClass.simpleName}")
//        }
//
//    private fun processSelect(select: Select): List<ColumnGraphNode> {
//        val selectBody = select.selectBody
//        return when (selectBody) {
//            is PlainSelect -> processPlainSelect(selectBody)
//            is SetOperationList -> {
//                // UNION, INTERSECT 등의 경우
//                selectBody.selects.flatMap { select ->
//                    when (select) {
//                        is PlainSelect -> processPlainSelect(select)
//                        else -> emptyList()
//                    }
//                }
//            }
//            else -> emptyList()
//        }
//    }
//
//    private fun processPlainSelect(plainSelect: PlainSelect): List<ColumnGraphNode> {
//        // 1. FROM 절에서 테이블과 서브쿼리 정보 수집
//        collectTableInfo(plainSelect.fromItem)
//        plainSelect.joins?.forEach { join ->
//            collectTableInfo(join.rightItem)
//        }
//
//        // 2. SELECT 절의 컬럼들 처리 (출력 컬럼들)
//        val outputNodes = mutableListOf<ColumnGraphNode>()
//        plainSelect.selectItems?.forEach { selectItem ->
//            val nodes = processSelectItem(selectItem)
//            outputNodes.addAll(nodes)
//            columnGraph.addAll(nodes)
//        }
//
//        // 3. WHERE, GROUP BY, HAVING, ORDER BY 절의 컬럼들 처리 (조건/정렬용 컬럼들)
//        val conditionNodes = mutableListOf<ColumnGraphNode>()
//
//        plainSelect.where?.let { whereExpression ->
//            val nodes = processExpression(whereExpression, "WHERE_CONDITION")
//            conditionNodes.addAll(nodes)
//            columnGraph.addAll(nodes)
//        }
//
//        plainSelect.groupBy?.groupByExpressions?.forEach { expr ->
//            val nodes = processExpression(expr, "GROUP_BY")
//            conditionNodes.addAll(nodes)
//            columnGraph.addAll(nodes)
//        }
//
//        plainSelect.having?.let { havingExpression ->
//            val nodes = processExpression(havingExpression, "HAVING_CONDITION")
//            conditionNodes.addAll(nodes)
//            columnGraph.addAll(nodes)
//        }
//
//        plainSelect.orderByElements?.forEach { orderByElement ->
//            val nodes = processExpression(orderByElement.expression, "ORDER_BY")
//            conditionNodes.addAll(nodes)
//            columnGraph.addAll(nodes)
//        }
//
//        return outputNodes + conditionNodes
//    }
//
//    private fun collectTableInfo(fromItem: FromItem?) {
//        when (fromItem) {
//            is Table -> {
//                val tableName = fromItem.name
//                val alias = fromItem.alias?.name ?: tableName
//
//                tables[alias] =
//                    TableInfo(
//                        alias = alias,
//                        originalName = tableName,
//                        isSubquery = false,
//                    )
//            }
//            is SubSelect -> {
//                val alias = fromItem.alias?.name ?: "subquery"
//
//                // 서브쿼리를 재귀적으로 처리하여 내부 컬럼 그래프 생성
//                val subqueryTracker = SqlColumnTracker()
//                val subqueryNodes =
//                    when (val selectBody = fromItem.selectBody) {
//                        is PlainSelect -> processPlainSelect(selectBody)
//                        is SetOperationList -> {
//                            // UNION, INTERSECT 등의 경우
//                            selectBody.selects.flatMap { select ->
//                                when (select) {
//                                    is PlainSelect -> processPlainSelect(select)
//                                    else -> emptyList()
//                                }
//                            }
//                        }
//                        else -> emptyList()
//                    }
//
//                tables[alias] =
//                    TableInfo(
//                        alias = alias,
//                        originalName = alias,
//                        isSubquery = true,
//                        subqueryNodes = subqueryNodes,
//                    )
//            }
//        }
//    }
//
//    private fun processSelectItem(selectItem: SelectItem): List<ColumnGraphNode> =
//        when (selectItem) {
//            is SelectExpressionItem -> {
//                val expression = selectItem.expression
//                val alias = selectItem.alias?.name
//
//                when (expression) {
//                    is Column -> {
//                        val outputName = alias ?: expression.columnName
//                        val source =
//                            ColumnSource(
//                                tableAlias = expression.table?.name,
//                                columnName = expression.columnName,
//                            )
//                        listOf(ColumnGraphNode(outputName, listOf(source)))
//                    }
//                    else -> {
//                        // 복합 표현식 (함수, 연산 등)
//                        val outputName = alias ?: "computed_column"
//                        val sources = extractSourcesFromExpression(expression)
//                        listOf(ColumnGraphNode(outputName, sources))
//                    }
//                }
//            }
//            is AllColumns -> {
//                // SELECT * 의 경우
//                tables.values.flatMap { tableInfo ->
//                    if (tableInfo.isSubquery) {
//                        // 서브쿼리의 경우, 서브쿼리의 출력 컬럼들을 사용
//                        tableInfo.subqueryNodes.map { subNode ->
//                            ColumnGraphNode(
//                                outputName = subNode.outputName,
//                                sources = subNode.sources,
//                            )
//                        }
//                    } else {
//                        // 실제 테이블의 경우, * 표현
//                        listOf(
//                            ColumnGraphNode(
//                                outputName = "*",
//                                sources = listOf(ColumnSource(tableInfo.alias, "*")),
//                            ),
//                        )
//                    }
//                }
//            }
//            is AllTableColumns -> {
//                // SELECT table.* 의 경우
//                val tableName = selectItem.table.name
//                val tableInfo = tables[tableName]
//
//                if (tableInfo?.isSubquery == true) {
//                    tableInfo.subqueryNodes.map { subNode ->
//                        ColumnGraphNode(
//                            outputName = subNode.outputName,
//                            sources = subNode.sources,
//                        )
//                    }
//                } else {
//                    listOf(
//                        ColumnGraphNode(
//                            outputName = "$tableName.*",
//                            sources = listOf(ColumnSource(tableName, "*")),
//                        ),
//                    )
//                }
//            }
//            else -> emptyList()
//        }
//
//    private fun processExpression(
//        expression: Expression,
//        context: String,
//    ): List<ColumnGraphNode> {
//        val sources = extractSourcesFromExpression(expression)
//        return if (sources.isNotEmpty()) {
//            listOf(
//                ColumnGraphNode(
//                    outputName = "${context}_${sources.joinToString("_") { "${it.tableAlias}.${it.columnName}" }}",
//                    sources = sources,
//                ),
//            )
//        } else {
//            emptyList()
//        }
//    }
//
//    private fun extractSourcesFromExpression(expression: Expression): List<ColumnSource> {
//        val sources = mutableListOf<ColumnSource>()
//
//        when (expression) {
//            is Column -> {
//                sources.add(
//                    ColumnSource(
//                        tableAlias = expression.table?.name,
//                        columnName = expression.columnName,
//                    ),
//                )
//            }
//            is BinaryExpression -> {
//                sources.addAll(extractSourcesFromExpression(expression.leftExpression))
//                sources.addAll(extractSourcesFromExpression(expression.rightExpression))
//            }
//            is Function -> {
//                expression.parameters?.expressions?.forEach { param ->
//                    sources.addAll(extractSourcesFromExpression(param))
//                }
//            }
//            is Parenthesis -> {
//                sources.addAll(extractSourcesFromExpression(expression.expression))
//            }
//            is CaseExpression -> {
//                expression.switchExpression?.let {
//                    sources.addAll(extractSourcesFromExpression(it))
//                }
//                expression.whenClauses?.forEach { whenClause ->
//                    sources.addAll(extractSourcesFromExpression(whenClause.whenExpression))
//                    sources.addAll(extractSourcesFromExpression(whenClause.thenExpression))
//                }
//                expression.elseExpression?.let {
//                    sources.addAll(extractSourcesFromExpression(it))
//                }
//            }
//            is SubSelect -> {
//                // 서브쿼리 내부의 컬럼들도 추적
//                val subqueryNodes =
//                    when (val selectBody = expression.selectBody) {
//                        is PlainSelect -> processPlainSelect(selectBody)
//                        is SetOperationList -> {
//                            // UNION, INTERSECT 등의 경우
//                            selectBody.selects.flatMap { select ->
//                                when (select) {
//                                    is PlainSelect -> processPlainSelect(select)
//                                    else -> emptyList()
//                                }
//                            }
//                        }
//                        else -> emptyList()
//                    }
//                subqueryNodes.forEach { node ->
//                    sources.addAll(node.sources)
//                }
//            }
//        }
//
//        return sources.distinct()
//    }
//
//    private fun flattenGraph(nodes: List<ColumnGraphNode>): List<ColumnDefinition> =
//        nodes.mapNotNull { node ->
//            flattenNode(node)
//        }
//
//    private fun flattenNode(node: ColumnGraphNode): ColumnDefinition? {
//        // 노드의 모든 소스를 추적하여 최종 원본 테이블과 컬럼을 찾음
//        val originalTables = mutableSetOf<String>()
//        val originalColumns = mutableSetOf<String>()
//        var primaryTableAlias: String? = null
//
//        node.sources.forEach { source ->
//            val result = traceToOriginal(source.tableAlias, source.columnName)
//            originalTables.addAll(result.originalTables)
//            originalColumns.addAll(result.originalColumns)
//            if (primaryTableAlias == null) {
//                primaryTableAlias = source.tableAlias
//            }
//        }
//
//        // 조건절 등에서 사용된 컬럼은 출력에 포함하지 않음 (선택적)
//        if (node.outputName.startsWith("WHERE_") ||
//            node.outputName.startsWith("GROUP_BY") ||
//            node.outputName.startsWith("HAVING_") ||
//            node.outputName.startsWith("ORDER_BY")
//        ) {
//            // 조건절 컬럼들도 포함하려면 이 부분을 제거
//            return ColumnDefinition(
//                table = primaryTableAlias,
//                orgTables = originalTables.toList(),
//                column = null, // 조건절에서는 출력 컬럼명이 없음
//                orgColumns = originalColumns.toList(),
//            )
//        }
//
//        return ColumnDefinition(
//            table = primaryTableAlias,
//            orgTables = originalTables.toList(),
//            column = node.outputName,
//            orgColumns = originalColumns.toList(),
//        )
//    }
//
//    private fun traceToOriginal(
//        tableAlias: String?,
//        columnName: String,
//    ): TraceResult {
//        if (tableAlias == null) {
//            // 테이블 접두사가 없는 경우, 첫 번째 테이블에서 찾음
//            val firstTable = tables.values.firstOrNull()
//            return if (firstTable != null) {
//                traceToOriginal(firstTable.alias, columnName)
//            } else {
//                TraceResult(emptySet(), setOf(columnName))
//            }
//        }
//
//        val tableInfo = tables[tableAlias]
//        if (tableInfo == null) {
//            return TraceResult(emptySet(), setOf(columnName))
//        }
//
//        if (!tableInfo.isSubquery) {
//            // 실제 테이블인 경우
//            return TraceResult(setOf(tableInfo.originalName), setOf(columnName))
//        }
//
//        // 서브쿼리인 경우, 서브쿼리 내부에서 해당 컬럼을 찾음
//        val matchingNode = tableInfo.subqueryNodes.find { it.outputName == columnName }
//        if (matchingNode != null) {
//            val originalTables = mutableSetOf<String>()
//            val originalColumns = mutableSetOf<String>()
//
//            matchingNode.sources.forEach { source ->
//                val result = traceToOriginalInSubquery(source, tableInfo.subqueryNodes)
//                originalTables.addAll(result.originalTables)
//                originalColumns.addAll(result.originalColumns)
//            }
//
//            return TraceResult(originalTables, originalColumns)
//        }
//
//        return TraceResult(emptySet(), setOf(columnName))
//    }
//
//    private fun traceToOriginalInSubquery(
//        source: ColumnSource,
//        subqueryNodes: List<ColumnGraphNode>,
//    ): TraceResult {
//        val originalTables = mutableSetOf<String>()
//        val originalColumns = mutableSetOf<String>()
//
//        // 서브쿼리 노드들 중에서 해당 컬럼을 찾음
//        val matchingNodes =
//            if (source.columnName == "*") {
//                // * 의 경우 모든 노드를 대상으로 함
//                subqueryNodes
//            } else {
//                // 특정 컬럼명과 매칭되는 노드들을 찾음
//                subqueryNodes.filter { node ->
//                    // 출력 컬럼명이 정확히 일치하거나
//                    node.outputName == source.columnName ||
//                        // 소스 중에 해당 컬럼이 포함되어 있는 경우
//                        node.sources.any { nodeSource ->
//                            nodeSource.columnName == source.columnName &&
//                                (source.tableAlias == null || nodeSource.tableAlias == source.tableAlias)
//                        }
//                }
//            }
//
//        if (matchingNodes.isEmpty()) {
//            // 매칭되는 노드가 없는 경우, 원본 소스 정보를 그대로 반환
//            return TraceResult(
//                originalTables = if (source.tableAlias != null) setOf(source.tableAlias) else emptySet(),
//                originalColumns = setOf(source.columnName),
//            )
//        }
//
//        // 찾은 노드들의 소스를 재귀적으로 추적
//        matchingNodes.forEach { node ->
//            node.sources.forEach { nodeSource ->
//                if (nodeSource.tableAlias != null) {
//                    // 테이블 별칭이 있는 경우, 해당 테이블이 서브쿼리인지 실제 테이블인지 확인
//                    val tableInfo = tables[nodeSource.tableAlias]
//
//                    when {
//                        tableInfo == null -> {
//                            // 테이블 정보가 없는 경우 (외부 참조일 수 있음)
//                            originalTables.add(nodeSource.tableAlias)
//                            originalColumns.add(nodeSource.columnName)
//                        }
//                        !tableInfo.isSubquery -> {
//                            // 실제 테이블인 경우
//                            originalTables.add(tableInfo.originalName)
//                            originalColumns.add(nodeSource.columnName)
//                        }
//                        else -> {
//                            // 중첩된 서브쿼리인 경우, 재귀적으로 추적
//                            val nestedResult = traceToOriginalInSubquery(nodeSource, tableInfo.subqueryNodes)
//                            originalTables.addAll(nestedResult.originalTables)
//                            originalColumns.addAll(nestedResult.originalColumns)
//                        }
//                    }
//                } else {
//                    // 테이블 별칭이 없는 경우, 컬럼명만 추가
//                    originalColumns.add(nodeSource.columnName)
//                }
//            }
//        }
//
//        // 결과가 없는 경우 원본 소스 정보를 사용
//        if (originalTables.isEmpty() && originalColumns.isEmpty()) {
//            return TraceResult(
//                originalTables = if (source.tableAlias != null) setOf(source.tableAlias) else emptySet(),
//                originalColumns = setOf(source.columnName),
//            )
//        }
//
//        return TraceResult(originalTables, originalColumns)
//    }
//
//    data class TraceResult(
//        val originalTables: Set<String>,
//        val originalColumns: Set<String>,
//    )
//
//    data class TableInfo(
//        val alias: String,
//        val originalName: String,
//        val isSubquery: Boolean = false,
//        val subqueryNodes: List<ColumnGraphNode> = emptyList(),
//    )
// }
