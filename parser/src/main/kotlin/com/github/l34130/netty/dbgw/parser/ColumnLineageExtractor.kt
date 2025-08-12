package com.github.l34130.netty.dbgw.parser

// class ColumnLineageExtractor {
//    fun extractColumnDefinitions(statement: Statement): List<ColumnDefinition> {
//        val definitions = mutableListOf<ColumnDefinition>()
//
//        statement.accept(
//            object : StatementVisitorAdapter() {
//                override fun visit(select: Select) {
//                    val selectBody = select.selectBody
//
//                    selectBody.accept(
//                        object : SelectVisitorAdapter() {
//                            override fun visit(plainSelect: PlainSelect) {
//                                // 1. Bottom-up: FROM/JOIN 절에서 테이블 정보부터 수집
//                                val tableFinder = TableFinder()
//                                plainSelect.fromItem.accept(tableFinder)
//                                plainSelect.joins?.forEach { join ->
//                                    join.rightItem.accept(tableFinder)
//                                }
//                                val tableMap = tableFinder.tableMap // ex) "u" to "users"
//
//                                // 2. 컬럼 분석기 준비
//                                val columnFinder = ColumnFinder(tableMap)
//
//                                // 3. SELECT 절의 컬럼들 분석
//                                plainSelect.selectItems.forEach { selectItem ->
//                                    selectItem.accept(
//                                        object : SelectItemVisitorAdapter() {
//                                            override fun visit(selectExpressionItem: SelectExpressionItem) {
//                                                val expression = selectExpressionItem.expression
//                                                expression.accept(columnFinder)
//
//                                                val sources = columnFinder.harvestSources()
//                                                if (sources.isNotEmpty()) {
//                                                    definitions.add(
//                                                        ColumnDefinition(
//                                                            // 쿼리에서 사용된 테이블 별칭 또는 이름. 여러 테이블 join 시 대표 하나를 표시하거나 null 처리 가능
//                                                            table = sources.first().tableAlias,
//                                                            orgTables = sources.mapNotNull { tableMap[it.tableAlias] }.distinct(),
//                                                            // 별칭이 있으면 별칭을, 없으면 원래 컬럼명을 사용
//                                                            column = selectExpressionItem.alias?.name ?: expression.toString(),
//                                                            orgColumns = sources.map { it.columnName }.distinct(),
//                                                        ),
//                                                    )
//                                                }
//                                            }
//                                            // SELECT * 와 같은 케이스는 이 예제에서 생략 (필요시 구현)
//                                        },
//                                    )
//                                }
//
//                                // 4. WHERE 절의 컬럼들 분석
//                                plainSelect.where?.let { where ->
//                                    where.accept(columnFinder)
//                                    val sources = columnFinder.harvestSources()
//                                    if (sources.isNotEmpty()) {
//                                        definitions.addAll(
//                                            sources.map { source ->
//                                                ColumnDefinition(
//                                                    table = source.tableAlias,
//                                                    orgTables = listOfNotNull(tableMap[source.tableAlias]),
//                                                    column = null, // WHERE 절의 컬럼은 최종 출력 컬럼이 아님
//                                                    orgColumns = listOf(source.columnName),
//                                                )
//                                            },
//                                        )
//                                    }
//                                }
//
//                                // JOIN ON, GROUP BY, ORDER BY 등의 컬럼도 필요시 위와 같은 방식으로 추가 분석 가능
//                            }
//                        },
//                    )
//                }
//            },
//        )
//
//        // 동일한 원본을 가지는 컬럼이 여러번 나올 수 있으므로 중복 제거
//        return definitions.distinct()
//    }
// }
//
// /**
// * FROM, JOIN 절을 방문하여 테이블 이름과 별칭을 수집하는 Visitor
// * tableMap("alias" to "tableName")
// */
// class TableFinder : FromItemVisitorAdapter() {
//    val tableMap = mutableMapOf<String, String>()
//
//    override fun visit(table: Table) {
//        val tableName = table.fullyQualifiedName
//        val key = table.alias?.name ?: tableName
//        tableMap[key] = tableName
//    }
// }
//
// // data class ColumnSource(
// //    val tableAlias: String?,
// //    val columnName: String,
// // )
//
// class ColumnFinder(
//    private val tableMap: Map<String, String>,
// ) : ExpressionVisitorAdapter() {
//    private val foundSources = mutableListOf<ColumnSource>()
//
//    fun harvestSources(): List<ColumnSource> =
//        foundSources.toList().apply {
//            foundSources.clear()
//        }
//
//    override fun visit(column: Column) {
//        val tableAlias = column.table?.name
//        val columnName = column.columnName
//
//        // 테이블 별칭이 없는 경우,
//        // FROM 절에 테이블이 하나만 있어야 명확하게 추적이 가능
//        if (tableAlias == null && tableMap.size == 1) {
//            val inferredAlias = tableMap.keys.first()
//            foundSources.add(ColumnSource(inferredAlias, columnName))
//        } else {
//            foundSources.add(ColumnSource(tableAlias, columnName))
//        }
//    }
// }
