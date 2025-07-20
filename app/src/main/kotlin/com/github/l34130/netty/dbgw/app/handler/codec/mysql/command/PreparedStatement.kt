package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

data class PreparedStatement(
    val query: String,
    val statementId: UInt,
    val columnDefinitions: List<ColumnDefinition41>,
    val parameterDefinitions: List<ColumnDefinition41>,
) {
    companion object {
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var query: String = ""
        private var statementId: UInt = 0u
        private var columnDefinitions: MutableList<ColumnDefinition41> = mutableListOf()
        private var parameterDefinitions: MutableList<ColumnDefinition41> = mutableListOf()

        fun query(query: String): Builder {
            this.query = query
            return this
        }

        fun statementId(statementId: UInt): Builder {
            this.statementId = statementId
            return this
        }

        fun addColumnDefinition(columnDefinition: ColumnDefinition41): Builder {
            this.columnDefinitions.add(columnDefinition)
            return this
        }

        fun addParameterDefinition(parameterDefinition: ColumnDefinition41): Builder {
            this.parameterDefinitions.add(parameterDefinition)
            return this
        }

        fun build(): PreparedStatement = PreparedStatement(query, statementId, columnDefinitions, parameterDefinitions)
    }
}
