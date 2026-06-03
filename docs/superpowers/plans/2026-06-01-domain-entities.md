# Domain Entities (Product & User) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement User and Product domain entities and their respective Spring Data JPA repositories, with verification through TDD.

**Architecture:** Use Spring Data JPA for data persistence. Entities will be mapped to a relational database. Users will be identified by email (from Google OAuth).

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data JPA, Lombok, H2 (for tests), JUnit 5.

---

### Task 1: Create User Entity

**Files:**
- Create: `backend/src/main/java/com/minicommerce/domain/User.java`

- [ ] **Step 1: Write User Entity**

```java
package com.minicommerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "users") // 'user' is reserved in PG
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email; // From Google OAuth
    
    @Column(nullable = false)
    private String name;
}
```

### Task 2: Create UserRepository with TDD

**Files:**
- Create: `backend/src/main/java/com/minicommerce/repository/UserRepository.java`
- Create: `backend/src/test/java/com/minicommerce/repository/UserRepositoryTests.java`

- [ ] **Step 1: Write the failing test for UserRepository**

```java
package com.minicommerce.repository;

import com.minicommerce.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindByEmail() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .build();
        userRepository.save(user);

        User found = userRepository.findByEmail("test@example.com");

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("test@example.com");
        assertThat(found.getName()).isEqualTo("Test User");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :backend:test --tests "com.minicommerce.repository.UserRepositoryTests"`
Expected: FAIL (Compilation error: UserRepository and User might not be fully integrated yet)

- [ ] **Step 3: Write minimal implementation of UserRepository**

```java
package com.minicommerce.repository;

import com.minicommerce.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :backend:test --tests "com.minicommerce.repository.UserRepositoryTests"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/minicommerce/domain/User.java backend/src/main/java/com/minicommerce/repository/UserRepository.java backend/src/test/java/com/minicommerce/repository/UserRepositoryTests.java
git commit -m "feat: add User entity and UserRepository"
```

### Task 3: Create Product Entity

**Files:**
- Create: `backend/src/main/java/com/minicommerce/domain/Product.java`

- [ ] **Step 1: Write Product Entity**

```java
package com.minicommerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private Integer price;
    
    @Column(nullable = false)
    private Integer stockQuantity;
}
```

### Task 4: Create ProductRepository with TDD

**Files:**
- Create: `backend/src/main/java/com/minicommerce/repository/ProductRepository.java`
- Create: `backend/src/test/java/com/minicommerce/repository/ProductRepositoryTests.java`

- [ ] **Step 1: Write the failing test for ProductRepository**

```java
package com.minicommerce.repository;

import com.minicommerce.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ProductRepositoryTests {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldSaveAndFindProduct() {
        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(100)
                .stockQuantity(10)
                .build();
        Product saved = productRepository.save(product);

        Product found = productRepository.findById(saved.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Test Product");
        assertThat(found.getPrice()).isEqualTo(100);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :backend:test --tests "com.minicommerce.repository.ProductRepositoryTests"`
Expected: FAIL (Compilation error)

- [ ] **Step 3: Write minimal implementation of ProductRepository**

```java
package com.minicommerce.repository;

import com.minicommerce.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :backend:test --tests "com.minicommerce.repository.ProductRepositoryTests"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/minicommerce/domain/Product.java backend/src/main/java/com/minicommerce/repository/ProductRepository.java backend/src/test/java/com/minicommerce/repository/ProductRepositoryTests.java
git commit -m "feat: add Product entity and ProductRepository"
```
