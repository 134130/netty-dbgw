# Parser Module

## Overview

This module is responsible for parsing SQL queries to analyze and determine data lineage. The primary goal is to identify which columns are being selected from and which are being referenced within a given SQL statement. This capability is essential for understanding data dependencies, performing impact analysis, and tracking the flow of data through the system.

## Architecture

The parser operates by first transforming the input SQL string into an Abstract Syntax Tree (AST). This tree is a structured representation of the query.

Once the AST is built, a set of `Visitor` classes traverse the tree. Each visitor is designed to handle a specific type of SQL statement (e.g., `SELECT`, `DELETE`). As the visitors navigate the nodes of the AST, they extract information about which columns are being accessed and how they are being used.

The final output is a structured result that details the selected columns and all referenced columns, providing a clear picture of the data lineage for the given query.

## Usage

To use the `SqlLineageAnalyzer`, create an instance of the class and call the `parse` method with a SQL string.

```kotlin
import com.github.l34130.netty.dbgw.parser.SqlLineageAnalyzer

fun main() {
    val analyzer = SqlLineageAnalyzer()
    val sql = "SELECT id, name FROM users WHERE age > 30"
    val result = analyzer.parse(sql)

    println("Selected Items: ${result.selectItems}")
    println("Referenced Columns: ${result.referencedColumns}")
}
```

### Result

The `parse` method returns a `ParseResult` object, which contains two sets:

*   `selectItems`: A set of objects representing the columns in the `SELECT` list. For the example above, this would effectively represent `id` and `name`.
*   `referencedColumns`: A set of objects representing all columns referenced in the query. For the example above, this would effectively represent `id`, `name`, and `age` from the `users` table.

## Value

The primary value of this module is its ability to provide clear insight into the data lineage of SQL queries. By programmatically identifying which columns are selected and referenced, this module enables:

*   **Impact Analysis:** Understand which queries will be affected by changes to a particular column.
*   **Data Governance:** Track the flow of sensitive data and ensure it is being handled appropriately.
*   **Dependency Mapping:** Visualize the relationships between tables and columns within the database.
*   **Developer Tooling:** Build tools that can provide developers with real-time feedback on their SQL queries.
