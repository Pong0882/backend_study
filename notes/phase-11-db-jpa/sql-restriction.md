# @SQLRestriction — Entity 레벨 조회 필터

## 왜 필요한가

Soft Delete를 구현하면 `deleted_at` 컬럼으로 탈퇴 여부를 관리한다.

문제는 JPA가 이 컬럼을 모른다는 것이다. `findByEmail`, `findAll`, `existsByNickname` 등 모든 조회에 `deleted_at IS NULL` 조건을 개발자가 직접 넣어야 한다.

```java
// 이렇게 하면 메서드마다 조건을 달아야 함
Optional<User> findByEmailAndDeletedAtIsNull(String email);
boolean existsByEmailAndDeletedAtIsNull(String email);
Optional<User> findByNicknameAndDeletedAtIsNull(String nickname);
boolean existsByNicknameAndDeletedAtIsNull(String nickname);
// 나중에 메서드 추가할 때 조건 빠뜨리면 탈퇴 유저 노출
```

메서드가 늘어날수록 누락 가능성이 커진다.

---

## @SQLRestriction

Entity 레벨에서 조회 조건을 한 번 선언하면 **모든 JPA 조회에 자동으로 WHERE 조건이 추가**된다.

```java
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
public class User { ... }
```

`findByEmail("test@test.com")` 호출 시 실제 나가는 쿼리:

```sql
SELECT * FROM users
WHERE email = 'test@test.com'
AND deleted_at IS NULL  -- 자동으로 추가됨
```

`findAll()` 호출 시:

```sql
SELECT * FROM users
WHERE deleted_at IS NULL  -- 자동으로 추가됨
```

Repository 메서드를 추가하거나 변경해도 조건 누락이 없다.

---

## @Where vs @SQLRestriction

Spring Boot 3.x (Hibernate 6) 이상에서 `@Where`가 deprecated되고 `@SQLRestriction`으로 교체됐다.

| | @Where | @SQLRestriction |
|---|---|---|
| 패키지 | `org.hibernate.annotations.Where` | `org.hibernate.annotations.SQLRestriction` |
| Hibernate 버전 | 5.x | 6.x (Spring Boot 3.x 이상) |
| 상태 | Deprecated | 현재 표준 |
| 사용법 | `@Where(clause = "deleted_at IS NULL")` | `@SQLRestriction("deleted_at IS NULL")` |

pong-to-rich는 Spring Boot 4.x (Hibernate 6.x)를 사용하므로 `@SQLRestriction`을 쓴다.

---

## 주의사항

### JPQL에서는 적용 안 됨

`@SQLRestriction`은 네이티브 SQL 레벨에서 동작한다. **JPQL에서는 자동으로 적용되지 않는다.**

```java
// JPQL — @SQLRestriction 적용 안 됨, 탈퇴 유저 포함될 수 있음
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmailJpql(@Param("email") String email);

// 이 경우 직접 조건을 넣어야 함
@Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
Optional<User> findByEmailJpql(@Param("email") String email);
```

Query Method(`findByEmail`)와 `JpaRepository` 기본 메서드(`findById`, `findAll`)는 적용된다.

### 탈퇴 유저를 조회해야 하는 경우

관리자 페이지에서 탈퇴 유저 목록을 보거나, 탈퇴 유저 데이터를 정리하는 배치 작업에서는 필터가 방해가 된다. 이 경우 네이티브 쿼리(`@Query(nativeQuery = true)`)를 쓰거나 별도 Repository를 만든다.

---

## pong-to-rich에서 사용된 곳

`User.java` — Soft Delete 구현에서 탈퇴 유저 자동 필터링

```java
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
public class User {
    // ...
    private LocalDateTime deletedAt;  // null = 정상, 값 있음 = 탈퇴

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }
}
```

`UserRepository`의 모든 조회 메서드에 `deleted_at IS NULL`이 자동 적용된다. 탈퇴 유저가 로그인, 닉네임 중복 체크 등에서 노출되는 것을 방지한다.
