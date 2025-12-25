dependencies {
    implementation(project(":aenigma-common")) // Common 모듈 사용

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client") // 또는 MySQL
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    testImplementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

// QueryDSL Q클래스 생성 경로 설정
val querydslDir = layout.buildDirectory.dir("generated/querydsl")

sourceSets {
    main {
        java {
            srcDirs("src/main/java", querydslDir)
        }
    }
}

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(querydslDir.get().asFile)
}

// clean 시 Q클래스도 삭제
tasks.named("clean") {
    doLast {
        delete(querydslDir)
    }
}