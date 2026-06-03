# Spring Boot Scaffolding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold a Spring Boot application with DB and Redis configuration.

**Architecture:** Standard Spring Boot project structure using Gradle for dependency management.

**Tech Stack:** Java, Spring Boot, Spring Data JPA, Spring Data Redis, PostgreSQL, Gradle, Lombok, Validation.

---

### Task 1: Create Build Configuration

**Files:**
- Create: `backend/build.gradle`

- [ ] **Step 1: Create `backend/build.gradle` with required dependencies**

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.minicommerce'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'org.postgresql:postgresql'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/build.gradle
git commit -m "chore: add build.gradle with Spring Boot dependencies"
```

---

### Task 2: Application Configuration

**Files:**
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Create `application.yml` with DB and Redis settings**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/minicommerce
    username: myuser
    password: mypassword
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  redis:
    host: localhost
    port: 6379
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/application.yml
git commit -m "chore: add application.yml configuration"
```

---

### Task 3: Application Entry Point and Smoke Test

**Files:**
- Create: `backend/src/main/java/com/minicommerce/MiniCommerceApplication.java`
- Create: `backend/src/test/java/com/minicommerce/MiniCommerceApplicationTests.java`

- [ ] **Step 1: Create the main application class**

```java
package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MiniCommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniCommerceApplication.class, args);
    }
}
```

- [ ] **Step 2: Create a context load test (smoke test)**

```java
package com.minicommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MiniCommerceApplicationTests {

    @Test
    void contextLoads() {
    }

}
```

- [ ] **Step 3: Create `backend/src/test/resources/application-test.yml` to avoid needing real DB/Redis for smoke test**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
  redis:
    host: localhost
    port: 6379
```

*Note: Adding H2 for testing is common to avoid needing infrastructure for unit/context tests.*

- [ ] **Step 4: Update `build.gradle` to include H2 for tests**

```gradle
dependencies {
    // ... existing
    testImplementation 'com.h2database:h2'
}
```

- [ ] **Step 5: Run the test**

```bash
cd backend && ./gradlew test
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/minicommerce/MiniCommerceApplication.java
git add backend/src/test/java/com/minicommerce/MiniCommerceApplicationTests.java
git add backend/src/test/resources/application-test.yml
git add backend/build.gradle
git commit -m "feat: add main application class and smoke test"
```
