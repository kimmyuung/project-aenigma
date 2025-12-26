dependencies {
    implementation(project(":aenigma-domain")) // Domain (및 Common) 사용
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    // WebSocket (STOMP)
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")
    
    // Security (JWT 인증용)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Discord Bot (JDA)
    implementation("net.dv8tion:JDA:5.0.0-beta.24")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket")
    testImplementation("org.springframework.security:spring-security-test")
}
// 이 모듈은 실제 서버로 뜔 것이므로 bootJar enabled = true (기본값)