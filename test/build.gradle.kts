plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    // Project "app" depends on project "util". (Project paths are separated with ":", so ":util" refers to the top-level "util" project.)
    implementation(project(":core"))
    implementation(project(":app"))
    implementation(project(":protocols:mysql"))
    implementation(project(":protocols:postgres"))
    implementation("io.netty:netty-all:4.2.2.Final")

    implementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("org.testcontainers:testcontainers:1.21.3")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:mysql:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testRuntimeOnly("com.mysql:mysql-connector-j:9.3.0")
    testRuntimeOnly("org.postgresql:postgresql:42.7.7")
    testImplementation(project(":policy-builtin"))
}
