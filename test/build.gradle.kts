plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":core"))
    implementation(project(":app"))
    implementation(project(":protocols:mysql"))
    implementation(project(":protocols:postgres"))
    implementation("io.netty:netty-all:4.2.2.Final")

    implementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:mysql:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testRuntimeOnly("com.mysql:mysql-connector-j:9.3.0")
    testRuntimeOnly("org.postgresql:postgresql:42.7.7")
}
