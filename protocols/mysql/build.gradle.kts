plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation("io.netty:netty-all:4.2.2.Final")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    testImplementation(kotlin("test"))
}
