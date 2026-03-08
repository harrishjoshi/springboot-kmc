# Security Configuration Guide

## Overview
This document outlines the security measures implemented in the authentication service and provides guidance for secure deployment.

## 🔐 Security Features

### 1. JWT Authentication
- **Algorithm**: HS256 (HMAC-SHA256)
- **Access Token Expiration**: 15 minutes (configurable)
- **Refresh Token Expiration**: 24 hours (configurable)
- **Token Validation**: Automatic validation on every request

### 2. Password Security
- **Algorithm**: BCrypt with default strength (cost factor: 10)
- **Policy**:
  - Minimum 8 characters
  - Maximum 128 characters
  - Must contain: uppercase, lowercase, digit, and special character (@$!%*?&)

### 3. Security Headers
All responses include security headers:
- **Content-Security-Policy**: Prevents XSS attacks
- **X-Frame-Options**: Prevents clickjacking (DENY)
- **X-Content-Type-Options**: Prevents MIME sniffing (nosniff)
- **Strict-Transport-Security**: Forces HTTPS (31536000 seconds)
- **X-XSS-Protection**: Legacy XSS filter (enabled)
- **Permissions-Policy**: Restricts access to browser features

### 4. CORS Configuration
- **Allowed Origins**: Configurable via `cors.allowed-origins` property
- **Default**: `http://localhost:3000`, `http://localhost:4200`
- **Allowed Methods**: GET, POST, PUT, DELETE, OPTIONS, PATCH
- **Credentials**: Enabled for authentication cookies/headers

### 5. SQL Injection Prevention
- All database queries use **Spring Data JPA** with parameterized queries
- No string concatenation in queries
- Repository methods use method naming conventions or @Query with named parameters

### 6. Exception Handling
- JWT exceptions caught and logged (no stack traces exposed to client)
- Generic error messages prevent information disclosure
- Detailed errors only logged server-side

---

##  Deployment Security Checklist

### Pre-Production
- [ ] **Generate strong JWT secret** (256-bit minimum)
- [ ] **Set environment variables** for secrets (don't use defaults)
- [ ] **Enable HTTPS** (disable HTTP)
- [ ] **Configure CORS** for your frontend domain(s)
- [ ] **Review allowed origins** in CorsConfig
- [ ] **Disable debug logging** (use production profile)
- [ ] **Run dependency audit** (`mvn dependency-check:check`)
- [ ] **Update dependencies** to latest secure versions
- [ ] **Configure firewall rules** (allow only necessary ports)
- [ ] **Setup monitoring** and alerting

### Production
- [ ] **JWT secret via secrets manager** (AWS Secrets Manager, HashiCorp Vault)
- [ ] **Database credentials encrypted**
- [ ] **TLS 1.2+ only** (disable TLS 1.0/1.1)
- [ ] **Rate limiting enabled** (API Gateway or application-level)
- [ ] **DDoS protection** (CloudFlare, AWS Shield)
- [ ] **Regular security audits**
- [ ] **Backup and disaster recovery** plan
- [ ] **Log aggregation** and monitoring (ELK, Splunk)

---

## 🔑 Environment Variables

### Required (Production)
```bash
# CRITICAL: Must be set in production!
export JWT_SECRET_KEY="<base64-encoded-256-bit-secret>"

# Database credentials
export DB_HOST="<database-host>"
export DB_PORT="5432"
export DB_NAME="auth"
export DB_USER="<db-username>"
export DB_PASSWORD="<db-password>"
```

### Optional
```bash
# JWT Configuration
export JWT_EXPIRATION="900000"           # 15 minutes in milliseconds
export JWT_REFRESH_EXPIRATION="86400000" # 24 hours in milliseconds

# CORS Configuration
export CORS_ALLOWED_ORIGINS="https://yourdomain.com,https://app.yourdomain.com"
```

### Generating Strong JWT Secret

```bash
# Generate 256-bit (32-byte) random secret and base64 encode it
openssl rand -base64 32

# Example output:
# 5K8yT3vN9xQ2wE7rY4uI1oP6aS8dF3gH2jK5lM9nB7vC=
```

---

## 🛡️ Security Best Practices

### 1. JWT Secret Management
- **Never commit secrets** to version control
- **Use secrets manager** in production (AWS Secrets Manager, Azure Key Vault)
- **Rotate secrets regularly** (every 90 days recommended)
- **Use different secrets** for different environments

### 2. Password Handling
- **Never log passwords** (already prevented in code)
- **Use HTTPS** to prevent password interception
- **Consider rate limiting** login attempts (not yet implemented)
- **Implement account lockout** after failed attempts (not yet implemented)

### 3. Token Management
- **Short access token lifetime** (15 minutes is good)
- **Longer refresh token lifetime** (24 hours, adjust as needed)
- **Implement token revocation** for logout (consider Redis blacklist)
- **Store tokens securely** on client (httpOnly cookies preferred over localStorage)

### 4. HTTPS/TLS
- **Always use HTTPS in production**
- **Enable HSTS header** (already configured)
- **Use strong cipher suites**
- **Keep certificates up to date**

### 5. Monitoring & Logging
- **Log all authentication events**
  - Successful logins
  - Failed login attempts
  - Token refresh operations
  - Access denied events
- **Monitor for suspicious activity**
  - Multiple failed logins from same IP
  - Unusual token usage patterns
  - Large number of 401/403 errors
- **Set up alerts** for security events

---

## 🔍 Security Testing

### Automated Testing
```bash
# Run OWASP Dependency Check
mvn dependency-check:check

# View report
open target/dependency-check-report.html
```

### Manual Testing
1. **Test password policy**:
   - Try weak passwords (should fail)
   - Try passwords without special chars (should fail)
   - Try valid strong password (should succeed)

2. **Test JWT validation**:
   - Try expired token (should return 401)
   - Try modified token (should return 401)
   - Try missing token (should return 401 for protected endpoints)

3. **Test security headers**:
   ```bash
   curl -I https://your-domain.com/api/v1/test/public
   # Should show security headers
   ```

4. **Test CORS**:
   - Try request from allowed origin (should succeed)
   - Try request from unauthorized origin (should be blocked)

5. **Test SQL injection** (should all fail):
   ```bash
   # Login with SQL injection attempt
   curl -X POST https://your-domain.com/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@test.com'\'' OR 1=1--","password":"anything"}'
   # Should return authentication error, not SQL error
   ```

---

## 📋 Compliance

### OWASP Top 10 2021 Compliance

| Risk | Status | Implementation |
|------|--------|----------------|
| A01: Broken Access Control |  COMPLIANT | Spring Security + @PreAuthorize annotations |
| A02: Cryptographic Failures |  COMPLIANT | BCrypt passwords, JWT signatures, HTTPS recommended |
| A03: Injection |  COMPLIANT | Parameterized queries via Spring Data JPA |
| A04: Insecure Design |  COMPLIANT | Secure-by-default configuration |
| A05: Security Misconfiguration |  COMPLIANT | Security headers, debug disabled in prod |
| A06: Vulnerable Components |  MONITOR | Run dependency checks regularly |
| A07: Authentication Failures |  PARTIAL | Strong password policy, consider rate limiting |
| A08: Data Integrity Failures |  COMPLIANT | JWT signature verification |
| A09: Logging Failures |  PARTIAL | Security events logged, enhance monitoring |
| A10: SSRF |  N/A | No external URL fetching |

---

## 🚨 Incident Response

### If JWT Secret is Compromised
1. **Immediately rotate** the JWT secret
2. **Force logout** all users (invalidate all existing tokens)
3. **Review logs** for suspicious activity
4. **Notify users** if necessary (depending on severity)

### If Database is Compromised
1. **Isolate** the database server
2. **Change all passwords** (database credentials)
3. **Audit access logs**
4. **Consider forcing** password reset for all users
5. **Review and patch** vulnerability

---

## 📞 Security Contacts

For security issues, please contact:
- **Security Team**: security@yourcompany.com
- **Emergency**: +1-XXX-XXX-XXXX

**Do not** open public GitHub issues for security vulnerabilities.

---

##  References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
