dependencies {
    implementation(project(":aenigma-domain")) // Domain (및 Common) 사용
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    // JPA (EntityScan, EnableJpaRepositories 사용을 위해)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    
    // Security & JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // OpenAPI (Swagger)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
}
// 이 모듈들은 실제 서버로 뜰 것이므로 bootJar enabled = true (기본값)