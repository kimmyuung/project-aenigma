dependencies {
    implementation(project(":aenigma-domain")) // Domain (및 Common) 사용
    implementation("org.springframework.boot:spring-boot-starter-web")
    // 각자 필요한 추가 라이브러리는 나중에 추가
}
// 이 모듈은 실제 서버로 뜰 것이므로 bootJar enabled = true (기본값)