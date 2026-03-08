# Security Configuration

## Overview
Security measures implemented in the authentication service.

## Security Features

### JWT Authentication
- Algorithm: HS256 (HMAC-SHA256)
- Access Token Expiration: Configurable
- Refresh Token Expiration: Configurable
- Token Validation: Automatic on every request

### Password Security
- Algorithm: BCrypt
- Policy:
  - Minimum 8 characters
  - Maximum 128 characters
  - Must contain: uppercase, lowercase, digit, and special character

### Security Headers
All responses include security headers:
- Content-Security-Policy: Prevents XSS attacks
- X-Frame-Options: Prevents clickjacking
- X-Content-Type-Options: Prevents MIME sniffing
- Strict-Transport-Security: Forces HTTPS
- X-XSS-Protection: Legacy XSS filter
- Permissions-Policy: Restricts access to browser features

### CORS Configuration
- Allowed Origins: Configurable via `cors.allowed-origins` property
- Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
- Credentials: Enabled for authentication

### SQL Injection Prevention
- All database queries use Spring Data JPA with parameterized queries
- No string concatenation in queries
- Repository methods use naming conventions or @Query with named parameters

### Exception Handling
- JWT exceptions caught and logged
- Generic error messages prevent information disclosure
- Detailed errors only logged server-side

## Deployment Checklist

### Pre-Production
- [ ] Generate strong JWT secret
- [ ] Set environment variables for secrets
- [ ] Enable HTTPS
- [ ] Configure CORS for your domains
- [ ] Disable debug logging
- [ ] Run dependency audit
- [ ] Update dependencies
- [ ] Configure firewall rules
- [ ] Setup monitoring and alerting

### Production
- [ ] JWT secret via secrets manager
- [ ] Database credentials encrypted
- [ ] TLS 1.2+ only
- [ ] Rate limiting enabled
- [ ] DDoS protection
- [ ] Regular security audits
- [ ] Backup and disaster recovery plan
- [ ] Log aggregation and monitoring

## Environment Variables

### Required
```bash
export JWT_SECRET_KEY="<base64-encoded-256-bit-secret>"
export DB_HOST="<database-host>"
export DB_PORT="5432"
export DB_NAME="auth"
export DB_USER="<db-username>"
export DB_PASSWORD="<db-password>"
```

### Optional
```bash
export JWT_EXPIRATION="900000"
export JWT_REFRESH_EXPIRATION="86400000"
export CORS_ALLOWED_ORIGINS="https://yourdomain.com"
```

### Generating JWT Secret
```bash
openssl rand -base64 32
```

## Security Best Practices

### JWT Secret Management
- Never commit secrets to version control
- Use secrets manager in production
- Rotate secrets regularly
- Use different secrets for different environments

### Password Handling
- Never log passwords
- Use HTTPS to prevent password interception
- Consider rate limiting login attempts
- Implement account lockout after failed attempts

### Token Management
- Short access token lifetime
- Longer refresh token lifetime
- Implement token revocation for logout
- Store tokens securely on client

### HTTPS/TLS
- Always use HTTPS in production
- Enable HSTS header
- Use strong cipher suites
- Keep certificates up to date

### Monitoring & Logging
- Log all authentication events
- Monitor for suspicious activity
- Set up alerts for security events

## Security Testing

### Automated Testing
```bash
mvn dependency-check:check
```

### Manual Testing
- Test password policy
- Test JWT validation
- Test security headers
- Test CORS
- Test SQL injection prevention

## OWASP Top 10 Compliance

| Risk | Status | Implementation |
|------|--------|----------------|
| A01: Broken Access Control | COMPLIANT | Spring Security + @PreAuthorize |
| A02: Cryptographic Failures | COMPLIANT | BCrypt passwords, JWT signatures |
| A03: Injection | COMPLIANT | Parameterized queries |
| A04: Insecure Design | COMPLIANT | Secure-by-default configuration |
| A05: Security Misconfiguration | COMPLIANT | Security headers configured |
| A06: Vulnerable Components | MONITOR | Run dependency checks regularly |
| A07: Authentication Failures | PARTIAL | Strong password policy |
| A08: Data Integrity Failures | COMPLIANT | JWT signature verification |
| A09: Logging Failures | PARTIAL | Security events logged |
| A10: SSRF | N/A | No external URL fetching |

## Incident Response

### If JWT Secret is Compromised
1. Immediately rotate the JWT secret
2. Force logout all users
3. Review logs for suspicious activity
4. Notify users if necessary

### If Database is Compromised
1. Isolate the database server
2. Change all passwords
3. Audit access logs
4. Consider forcing password reset for all users
5. Review and patch vulnerability
