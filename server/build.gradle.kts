plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.1.1"
    application
}

group = "com.sourcehub.server"
version = "1.0"

application {
    mainClass.set("com.sourcehub.server.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:3.1.1")
    implementation("io.ktor:ktor-server-netty:3.1.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.1")
    implementation("io.ktor:ktor-server-auth:3.1.1")
    implementation("io.ktor:ktor-server-auth-jwt:3.1.1")
    implementation("io.ktor:ktor-server-status-pages:3.1.1")
    implementation("io.ktor:ktor-server-cors:3.1.1")
    implementation("io.ktor:ktor-server-compression:3.1.1")
    implementation("io.ktor:ktor-server-default-headers:3.1.1")

    // Serialization (Gson — handles heterogeneous maps)
    implementation("io.ktor:ktor-serialization-gson:3.1.1")
    implementation("com.google.code.gson:gson:2.11.0")

    // Database (Exposed ORM + H2 for local dev)
    implementation("org.jetbrains.exposed:exposed-core:0.49.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.49.0")
    runtimeOnly("com.h2database:h2:2.2.224")

    // PostgreSQL (for cloud deployment, swap runtime dependency)
    // runtimeOnly("org.postgresql:postgresql:42.7.3")

    // Security
    implementation("io.ktor:ktor-server-auth-jwt:3.1.1")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("com.auth0:java-jwt:4.4.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:3.1.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
}
