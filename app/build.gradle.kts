plugins {
    id("java")
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":core"))
    implementation(project(":protocols:mysql"))

    implementation("com.xenomachina:kotlin-argparser:2.0.7")

    implementation("io.netty:netty-all:4.2.2.Final")

    implementation("org.bouncycastle:bcpkix-jdk18on:1.81")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(kotlin("test"))
}

application {
    mainClass = "com.github.l34130.netty.dbgw.app.AppKt"
}
