dependencies {
    implementation(project(":aenigma-domain")) // Domain (및 Common) 사용
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
// 이 모듈은 실제 서버로 뜰 것이므로 bootJar enabled = true (기본값)