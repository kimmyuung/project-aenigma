dependencies {
    implementation(project(":aenigma-common")) // Common 모듈 사용

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client") // 또는 MySQL
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
tasks.bootJar { enabled = false }
tasks.jar { enabled = true }