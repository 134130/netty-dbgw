plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    implementation(project(":common"))
    api(project(":policy"))

    // runTimeonly is correct here, but IntelliJ IDEA does not recognize it as such.
    implementation(project(":policy-builtin"))

    implementation(libs.bundles.kotlinxEcosystem)
    implementation("io.netty:netty-all:4.2.2.Final")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.81")
}
