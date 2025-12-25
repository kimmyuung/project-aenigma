dependencies {
    // 공통으로 쓸 라이브러리 (예: Jackson, Apache Commons 등) 필요시 추가
}
tasks.bootJar { enabled = false } // 라이브러리 모듈이므로 실행 파일 생성 X
tasks.jar { enabled = true }