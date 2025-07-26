package com.github.l34130.netty.dbgw.policy.api

import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ResourceManifestMapperTest {
    @Test
    fun `test readValues`() {
        val yaml =
            """
            apiVersion: builtin/v1
            kind: DatabaseStatementTypePolicy
            metadata:
              name: reject-modify-stmt-policy
            spec:
              statements:
                - "DELETE"
                - "UPDATE"
              action: DENY
            """.trimIndent()

        val parsed = ResourceManifestMapper.readValues(yaml)

        assertEquals(1, parsed.size)
        val manifest = parsed[0]
        assertAll({
            assertEquals("builtin/v1", manifest.apiVersion)
        }, {
            assertEquals("DatabaseStatementTypePolicy", manifest.kind)
        }, {
            assertEquals("reject-modify-stmt-policy", manifest.metadata["name"])
        }, {
            assertContentEquals(listOf("DELETE", "UPDATE"), manifest.spec["statements"] as List<*>)
        }, {
            assertEquals("DENY", manifest.spec["action"])
        })
    }

    @Test
    fun `test writeValueAsString`() {
        val manifest =
            Manifest(
                apiVersion = "builtin/v1",
                kind = "DatabaseStatementTypePolicy",
                metadata = mapOf("name" to "reject-modify-stmt-policy"),
                spec =
                    mapOf(
                        "statements" to listOf("DELETE", "UPDATE"),
                        "action" to "DENY",
                    ),
            )

        val yaml = ResourceManifestMapper.writeValueAsString(manifest)
        assertEquals(
            """
            apiVersion: "builtin/v1"
            kind: "DatabaseStatementTypePolicy"
            metadata:
              name: "reject-modify-stmt-policy"
            spec:
              statements:
                - "DELETE"
                - "UPDATE"
              action: "DENY"
            
            """.trimIndent(),
            yaml,
        )
    }

    @Test
    fun `test writeValuesAsString`() {
        val manifests =
            listOf(
                Manifest(
                    apiVersion = "builtin/v1",
                    kind = "DatabaseStatementTypePolicy",
                    metadata = mapOf("name" to "reject-select-stmt-policy"),
                    spec =
                        mapOf(
                            "statements" to listOf("DELETE", "UPDATE"),
                            "action" to "DENY",
                        ),
                ),
                Manifest(
                    apiVersion = "builtin/v1",
                    kind = "DatabaseStatementTypePolicy",
                    metadata = mapOf("name" to "allow-modify-stmt-policy"),
                    spec =
                        mapOf(
                            "statements" to listOf("SELECT"),
                            "action" to "ALLOW",
                        ),
                ),
            )

        val yaml = ResourceManifestMapper.writeValuesAsString(manifests)
        assertEquals(
            """
            apiVersion: "builtin/v1"
            kind: "DatabaseStatementTypePolicy"
            metadata:
              name: "reject-select-stmt-policy"
            spec:
              statements:
                - "DELETE"
                - "UPDATE"
              action: "DENY"
            ---
            apiVersion: "builtin/v1"
            kind: "DatabaseStatementTypePolicy"
            metadata:
              name: "allow-modify-stmt-policy"
            spec:
              statements:
                - "SELECT"
              action: "ALLOW"
            
            """.trimIndent(),
            yaml,
        )
    }
}
