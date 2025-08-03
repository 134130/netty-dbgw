plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":core"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation("io.netty:netty-all:4.2.2.Final")
}
