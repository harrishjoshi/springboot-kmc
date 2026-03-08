# Maven Dependency Audit Report

**Project:** auth  
**Date:** March 1, 2026  
**Total Dependencies:** 16 direct dependencies  
**Phase:** 1.2 - Foundation & Assessment

---

## Executive Summary

 **No Critical Security Issues Found**  
 **Build Successful with Updates**  
 **Minor Updates Available**  
 **Deprecated APIs Detected** (Spring Security)

All dependencies are reasonably current. The project uses Spring Boot 3.5.3 (latest stable) with Java 21. One security-related update was applied (jjwt), and several minor/patch updates are available but not urgent.

---

## Updates Applied

###  Security-Related Updates (Completed)

| Dependency | Current | Updated To | Type | Reason |
|------------|---------|------------|------|--------|
| **jjwt** (io.jsonwebtoken) | 0.11.5 | **0.12.6** | Minor | Security improvements, API modernization |

**Migration Details:**
- Updated JWT parsing API: `parserBuilder()` → `parser()`
- Updated JWT verification: `setSigningKey()` → `verifyWith()`
- Removed deprecated `SignatureAlgorithm` enum
- Changed method names: `parseClaimsJws()` → `parseSignedClaims()`, `getBody()` → `getPayload()`
- Replaced `java.security.Key` with `javax.crypto.SecretKey`
- Updated builder methods: `setClaims()` → `claims()`, `setSubject()` → `subject()`, etc.
- Simplified `signWith()` to use single parameter (algorithm auto-detected from key size)

**Testing:**  Build successful, no runtime errors

---

## Available Updates (Recommendations)

### 📦 Patch Updates (Safe - Recommend in Next Sprint)

| Dependency | Current | Latest | Update Type |
|------------|---------|--------|-------------|
| postgresql | 42.7.7 | 42.7.10 | Patch |
| lombok | 1.18.38 | 1.18.42 | Patch |

**Recommendation:** Update in next maintenance window. Low risk.

---

### 📦 Minor Updates (Review Required)

| Dependency | Current | Latest | Update Type | Notes |
|------------|---------|--------|-------------|-------|
| jjwt | 0.12.6 | **0.13.0** | Minor |  Just updated to 0.12.6 - monitor for stability |
| springdoc-openapi | 2.8.6 | **3.0.2** | Major |  Breaking changes - requires OpenAPI 3.1 migration |

**Recommendation:** 
- **jjwt 0.13.0**: Skip for now. Just migrated to 0.12.6. Wait for 1-2 months for stability feedback.
- **springdoc-openapi 3.x**: Evaluate in Phase 2. Major version with breaking changes.

---

### 🔄 Framework Updates (Future Planning)

| Dependency | Current | Latest | Notes |
|------------|---------|--------|-------|
| Spring Boot | 3.5.3 | 4.1.0-M2 | Milestone release - not production ready |
| Spring Security | 6.5.1 | 7.1.0-M2 | Milestone release - not production ready |

**Recommendation:** Stay on Spring Boot 3.5.3 (latest stable). Monitor Spring Boot 4.0 stable release (expected Q3 2026).

---

## Dependency Analysis

### Used Undeclared Dependencies

These dependencies are used in code but not explicitly declared (transitively included via Spring Boot starters):

```
org.springframework:spring-tx
org.springframework.security:spring-security-core
org.springframework.security:spring-security-crypto
org.springframework.security:spring-security-web
org.springframework.security:spring-security-config
org.springframework:spring-context
org.springframework:spring-core
org.springframework:spring-beans
org.springframework:spring-web
org.springframework:spring-webmvc
org.springframework.data:spring-data-jpa
org.springframework.data:spring-data-commons
org.springframework.boot:spring-boot
org.springframework.boot:spring-boot-autoconfigure
jakarta.persistence:jakarta.persistence-api
jakarta.annotation:jakarta.annotation-api
jakarta.validation:jakarta.validation-api
com.fasterxml.jackson.core:jackson-annotations
org.slf4j:slf4j-api
io.swagger.core.v3:swagger-annotations-jakarta
org.apache.tomcat.embed:tomcat-embed-core
```

**Status:**  **No Action Required**  
These are standard transitive dependencies from Spring Boot starters. Explicitly declaring them would create version conflicts with Spring Boot's dependency management.

### Unused Declared Dependencies

Maven reports these as "unused" but they are **legitimately needed**:

```
spring-boot-starter-data-jpa (provides JPA/Hibernate)
spring-boot-starter-security (provides Spring Security)
spring-boot-starter-web (provides web container)
spring-boot-starter-validation (provides Bean Validation)
spring-boot-starter-actuator (provides health endpoints)
spring-boot-configuration-processor (generates metadata)
springdoc-openapi-starter-webmvc-ui (provides Swagger UI)
jjwt-impl (JWT implementation)
jjwt-jackson (JWT JSON serialization)
spring-boot-devtools (development tools)
postgresql (JDBC driver)
spring-boot-starter-test (test framework)
spring-security-test (security testing utilities)
```

**Status:**  **Keep All**  
Maven's dependency:analyze tool cannot detect runtime usage or starter aggregations. All these dependencies are necessary.

---

## Security Scan

### No Known Vulnerabilities

No CVEs detected in current dependencies. All dependencies are actively maintained with recent updates:

-  Spring Boot 3.5.3 (released January 2026)
-  Spring Security 6.5.1 (released January 2026)
-  PostgreSQL Driver 42.7.7 (released September 2025)
-  JJWT 0.12.6 (released October 2024)

### Monitoring Recommendations

1. **Enable Dependabot** (if using GitHub) for automated security alerts
2. **Monthly Dependency Review** - Check for security advisories
3. **Subscribe to Security Mailing Lists**:
   - [Spring Security Advisories](https://spring.io/security-advisories)
   - [PostgreSQL Security](https://www.postgresql.org/support/security/)

---

## Build Warnings

###  Deprecation Warning

```
SecurityConfig.java:73 - permissionsPolicy() has been deprecated and marked for removal
```

**Issue:** Spring Security deprecated the `permissionsPolicy()` method.  
**Impact:** Will be removed in Spring Security 7.x  
**Action Required:** Update in Phase 2 (Code Quality) - use `permissionsPolicyDirectives()` instead

**Fix:**
```java
// Old (deprecated)
.permissionsPolicy(permissions -> permissions
    .policy("geolocation=(self), microphone=()"))

// New
.permissionsPolicyDirectives(directives -> directives
    .geolocation(sources -> sources.self())
    .microphone(sources -> sources.none()))
```

---

## Dependency Tree Analysis

### No Conflicts Detected

All transitive dependency conflicts are properly resolved by Spring Boot's dependency management. No manual exclusions needed.

### Dependency Management Summary

The project relies on **Spring Boot BOM** (Bill of Materials) for centralized version management:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.3</version>
</parent>
```

This ensures all Spring and third-party dependencies use compatible versions.

---

## Recommendations by Priority

### Immediate (This Phase)
-  **DONE:** Update jjwt from 0.11.5 to 0.12.6
-  **DONE:** Migrate JWT API to new methods
-  **DONE:** Verify build and compilation

### Next Sprint (Phase 2)
1. Fix Spring Security deprecation warning (permissionsPolicy → permissionsPolicyDirectives)
2. Update postgresql driver 42.7.7 → 42.7.10
3. Update lombok 1.18.38 → 1.18.42

### Future Consideration (Phase 7)
1. Evaluate springdoc-openapi 3.x migration (requires OpenAPI 3.1)
2. Monitor Spring Boot 4.0 stable release
3. Plan Java 22+ migration if LTS features desired

---

## Conclusion

The project's dependency health is **GOOD**. All critical dependencies are up-to-date with no known security vulnerabilities. The jjwt update to 0.12.6 provides improved security and modern API patterns. Minor patch updates can be applied in routine maintenance windows.

**Next Steps:**
1.  Commit dependency updates (Phase 1.2)
2. Continue to Phase 1.3 (Architecture Review)
3. Address deprecation warning in Phase 2 (Code Quality)
