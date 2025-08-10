plugins {
    id("java")
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application
}

dependencies {
    // Project "app" depends on project "util". (Project paths are separated with ":", so ":util" refers to the top-level "util" project.)
    implementation(project(":core"))
    implementation(project(":protocols:mysql"))
    implementation(project(":protocols:postgres"))

    implementation("com.xenomachina:kotlin-argparser:2.0.7")

    implementation("io.netty:netty-all:4.2.2.Final")
}

application {
    mainClass = "com.github.l34130.netty.dbgw.app.AppKt"
}
