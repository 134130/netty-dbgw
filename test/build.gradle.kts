plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":core"))
    implementation(project(":app"))
    implementation(project(":protocols:mysql"))
    implementation("io.netty:netty-all:4.2.2.Final")

    implementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:mysql:1.21.3")
    testRuntimeOnly("com.mysql:mysql-connector-j:9.3.0")
}
