plugins {
    java
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7" // 스프링 의존성 관리자
}

allprojects {
    group = "com.aenigma"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// 하위 모듈들에 공통으로 적용될 설정
subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21) // Java 21 강제
        }
    }

    dependencies {
        // 모든 모듈에서 Lombok 사용
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // 모든 모듈에서 테스트 기능 사용
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// 루트 프로젝트 자체는 실행 파일(jar)을 만들지 않음
tasks.bootJar { enabled = false }
tasks.jar { enabled = false }