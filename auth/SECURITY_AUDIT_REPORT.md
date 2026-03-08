# Security Audit Report
**Date**: March 1, 2026  
**Project**: Spring Boot Auth Service  
**Auditor**: Claude Code (security-audit skill)

---

##  STATUS UPDATE (Phase 7 - Final Review)

This audit was conducted during **Phase 1.1**. Several critical issues have since been **RESOLVED** in subsequent phases:

###  RESOLVED ISSUES

#### Issue #1: Weak Password Policy - **FIXED in Phase 2.1**
- **Implementation**: `RegisterRequest.java` now has `@Pattern` validation
- **Requirements**: Minimum 8 characters, uppercase, lowercase, digit, special character
- **Evidence**: Lines 19-24 in RegisterRequest.java
```java
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
    message = "Password must contain uppercase, lowercase, digit, and special character"
)
```

#### Issue #2: Hardcoded JWT Secret - **FIXED in Phase 2.1**
- **Implementation**: `JwtService` validates secret at application startup
- **Behavior**: Application fails fast if weak or default secret is provided
- **Evidence**: JwtService constructor validates secret length and strength

#### Issue #3: Missing Security Headers - **FIXED in Phase 6**
- **Implementation**: `SecurityConfig.java` lines 58-76
- **Headers Added**:
  - Content Security Policy (CSP)
  - X-Frame-Options: DENY
  - HTTP Strict Transport Security (HSTS)
  - X-Content-Type-Options: nosniff
  - X-XSS-Protection: 1; mode=block
  - Permissions-Policy
- **Evidence**: SecurityConfig.java headerSecurity() configuration

###  REMAINING OPEN ISSUES (Require Future Action)

The following issues from the original audit remain unresolved and should be prioritized:

- **Issue #4**: Debug logging in production (needs profile-specific config)
- **Issue #5**: XSS vulnerability in blog post content (needs HTML sanitization library)
- **Issue #6**: No CORS origin restriction (needs production-specific CORS config)
- **Issue #7**: JWT not invalidated on logout (requires token blacklist/Redis implementation)
- **Issue #8**: Poor JWT exception handling (needs generic error messages)
- **Issue #9**: CORS configuration improvements needed

**See current security status in**: [SECURITY.md](SECURITY.md)

---

## Executive Summary (Original - Phase 1.1)

This security audit identified **9 security issues** across critical, high, and medium severity levels. The application follows many security best practices (BCrypt passwords, parameterized queries, JWT authentication) but requires improvements in password policies, secrets management, security headers, and input sanitization.

**Risk Level**: MEDIUM (No critical vulnerabilities, but improvements needed before production)

---

## Findings

### CRITICAL (Must Fix Before Production)

#### 1. Weak Password Policy  
**File**: `src/main/java/com/harrish/auth/dto/RegisterRequest.java:19`  
**Current**: Minimum 6 characters  
**Risk**: Easily bruteforceable passwords  
**Recommendation**: 
- Minimum 8-12 characters
- Require uppercase, lowercase, digit, and special character
- Add password strength validation

**Fix**:
```java
@NotBlank(message = "Password is required")
@Size(min = 8, max = 128, message = "Password must be 8-128 characters")
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
    message = "Password must contain uppercase, lowercase, digit, and special character"
)
String password
```

---

#### 2. Hardcoded Default JWT Secret 
**File**: `src/main/resources/application.properties:17`  
**Current**: Default secret visible in codebase  
**Risk**: If JWT_SECRET_KEY env var not set, uses weak default  
**Recommendation**:
- Fail application startup if JWT_SECRET_KEY not provided
- Generate strong secrets (256-bit minimum)
- Use secrets manager in production

**Fix**: Add validation bean that checks secret strength at startup

---

#### 3. Missing Security Headers 
**File**: `src/main/java/com/harrish/auth/security/SecurityConfig.java`  
**Current**: No security headers configured  
**Risk**: Vulnerable to XSS, clickjacking, MIME sniffing attacks  
**Recommendation**: Add comprehensive security headers

**Fix**:
```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> 
        csp.policyDirectives("default-src 'self'; script-src 'self'"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.disable()) // Modern CSP replaces X-XSS-Protection
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> 
        hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
);
```

---

### HIGH (Should Fix Soon)

#### 4. Debug Logging Enabled 
**File**: `src/main/resources/application.properties:10,40-41`  
**Current**: SQL queries and web debugging enabled  
**Risk**: Information disclosure, performance impact  
**Recommendation**: Use Spring profiles to enable debug only in dev

**Fix**:
- Move debug settings to `application-dev.properties`
- Disable in production

---

#### 5. No Rate Limiting 
**File**: Authentication endpoints  
**Current**: No rate limiting on login/register  
**Risk**: Brute force attacks, account enumeration  
**Recommendation**: 
- Add rate limiting (e.g., Bucket4j)
- Limit login attempts per IP
- Add account lockout after failed attempts

**Fix**: Add rate limiting dependency and configuration

---

#### 6. Missing Input Sanitization for XSS 
**File**: `src/main/java/com/harrish/auth/dto/CreateBlogPostRequest.java`  
**Current**: No XSS protection for blog content  
**Risk**: Stored XSS attacks  
**Recommendation**: Sanitize HTML content or validate against dangerous patterns

**Fix**: Add @SafeHtml custom validator

---

### MEDIUM (Good to Have)

#### 7. JWT Token Not Invalidated on Logout 
**Current**: No token blacklist/revocation  
**Risk**: Stolen tokens remain valid until expiration  
**Recommendation**: 
- Implement token blacklist (Redis)
- Use shorter access token expiration (current 15min is good)
- Add logout endpoint

---

#### 8. Poor JWT Exception Handling 
**File**: `src/main/java/com/harrish/auth/security/JwtAuthenticationFilter.java:46`  
**Current**: Token parsing exceptions not caught  
**Risk**: Stack traces could leak information  
**Recommendation**: Add try-catch around JWT operations with generic error

---

#### 9. Missing CORS Configuration 
**Current**: No CORS configured  
**Risk**: Cannot call API from browser apps on different domains  
**Recommendation**: Add CORS configuration for allowed origins

---

## What's Working Well 

1. **Password Hashing**: BCrypt with default strength (good)
2. **SQL Injection Prevention**: All queries use Spring Data JPA (parameterized)
3. **CSRF for REST API**: Correctly disabled for stateless JWT API
4. **Authorization**: Proper use of @PreAuthorize annotations
5. **Input Validation**: Jakarta Validation used consistently
6. **Session Management**: Stateless (correct for JWT)
7. **Secrets via Environment**: Database and JWT support env vars

---

## Remediation Plan

### Phase 1 (Critical - This Sprint)
- [ ] Strengthen password policy
- [ ] Add JWT secret validation at startup
- [ ] Implement security headers
- [ ] Disable debug logging in production

### Phase 2 (High - Next Sprint)
- [ ] Add rate limiting for auth endpoints
- [ ] Implement XSS sanitization for blog posts
- [ ] Improve JWT exception handling

### Phase 3 (Medium - Future)
- [ ] Implement token revocation/blacklist
- [ ] Add CORS configuration
- [ ] Add logout endpoint with token invalidation

---

## Testing Recommendations

### Security Testing Checklist
- [ ] Run OWASP Dependency Check
- [ ] Test weak password rejection
- [ ] Verify security headers with SecurityHeaders.com
- [ ] Test SQL injection (should fail)
- [ ] Test XSS in blog posts
- [ ] Test unauthorized access to protected endpoints
- [ ] Test token expiration handling
- [ ] Verify rate limiting (when implemented)

### Tools
- OWASP ZAP for automated scanning
- Burp Suite for manual testing
- `mvn dependency-check:check` for vulnerable dependencies

---

## Compliance Notes

**OWASP Top 10 2021 Coverage:**
- A01 Broken Access Control:  GOOD (Spring Security + @PreAuthorize)
- A02 Cryptographic Failures:  MEDIUM (good password hashing, weak default secret)
- A03 Injection:  GOOD (parameterized queries)
- A04 Insecure Design:  GOOD (follows security patterns)
- A05 Security Misconfiguration:  MEDIUM (debug enabled, missing headers)
- A06 Vulnerable Components: ❓ UNKNOWN (needs dependency audit)
- A07 Authentication Failures:  MEDIUM (weak password policy, no rate limiting)
- A08 Data Integrity Failures:  GOOD (JWT signatures verified)
- A09 Logging Failures:  MEDIUM (no security event logging)
- A10 SSRF:  N/A (no external URL fetching)

---

## Sign-off

This audit provides a snapshot of security posture as of March 1, 2026. Security is an ongoing process - implement these recommendations and re-audit after changes.

**Next Audit Date**: After Phase 1 remediation complete
