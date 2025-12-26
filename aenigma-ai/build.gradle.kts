dependencies {
    implementation(project(":aenigma-domain"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // AI/ML 관련 - Spring AI 저장소 설정 필요
    // implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0-M4")
    
    // JSON 처리
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}