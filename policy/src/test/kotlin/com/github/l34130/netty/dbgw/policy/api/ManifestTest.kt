package com.github.l34130.netty.dbgw.policy.api

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class ManifestTest {
    @TestFactory
    fun `test decodeYaml`() =
        listOf(
            dynamicTest("parse DatabaseStatementTypePolicy") {
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

                val parsed = ResourceManifestReader.readFrom(yaml)
            },
        )

    @TestFactory
    fun `test encodeYaml`() =
        listOf(
            dynamicTest("encode a policy") {
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

                val yaml = ResourceManifestEncoder.encodeYaml(manifest)
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
            },
            dynamicTest("encode multiple policies") {
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

                val yaml = ResourceManifestEncoder.encodeYaml(manifests)
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
            },
        )
}
