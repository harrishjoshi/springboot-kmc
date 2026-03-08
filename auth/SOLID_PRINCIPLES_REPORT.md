# SOLID Principles Review Report

**Project**: Spring Boot Authentication Service  
**Review Date**: March 1, 2026  
**Phase**: 2.2 - Code Quality & Design  
**Reviewer**: Claude Code (using solid-principles skill)

---

## Executive Summary

This report analyzes the Spring Boot authentication service codebase for adherence to SOLID principles. The codebase shows good dependency injection practices but has several areas where SOLID principles are violated, particularly around Single Responsibility and Dependency Inversion.

### Overall Assessment

| Principle | Score | Status |
|-----------|-------|--------|
| Single Responsibility (S) | 6/10 |  Needs Improvement |
| Open/Closed (O) | 7/10 |  Good |
| Liskov Substitution (L) | 8/10 |  Good |
| Interface Segregation (I) | 7/10 |  Good |
| Dependency Inversion (D) | 6/10 |  Needs Improvement |
| **Overall** | **6.8/10** |  **Needs Improvement** |

### Violations Summary

| Principle | Violations | Critical | High | Medium | Low |
|-----------|------------|----------|------|--------|-----|
| Single Responsibility (S) | 6 | 0 | 3 | 3 | 0 |
| Open/Closed (O) | 4 | 0 | 1 | 2 | 1 |
| Liskov Substitution (L) | 2 | 0 | 0 | 1 | 1 |
| Interface Segregation (I) | 2 | 0 | 0 | 1 | 1 |
| Dependency Inversion (D) | 5 | 0 | 0 | 3 | 2 |
| **TOTAL** | **19** | **0** | **4** | **10** | **5** |

---

## 1. Single Responsibility Principle (SRP) Violations

> "A class should have only one reason to change."

###  HIGH: User Model - Multiple Responsibilities

**File**: `src/main/java/com/harrish/auth/model/User.java:22-97`  
**Severity**: HIGH  
**Impact**: Maintainability, Testability

#### Issue Description

The `User` class violates SRP by combining three distinct responsibilities:

1. **Domain entity** (JPA persistence - lines 16-41)
2. **Spring Security integration** (UserDetails implementation - lines 43-72)
3. **Identity management** (equals/hashCode/toString - lines 74-96)

When Spring Security requirements change, authentication logic changes, or domain requirements change, this class needs to be modified.

```java
//  CURRENT: User does too much
@Entity
@Table(name = "users")
public class User extends Auditable implements UserDetails {
    // JPA fields
    @Id @GeneratedValue
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    
    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    // ... more UserDetails methods
}
```

#### Refactoring Recommendation

**Priority**: HIGH  
**Effort**: Medium (2-3 hours)

```java
//  REFACTORED: Separate concerns

// 1. User.java (pure domain entity)
@Entity
@Table(name = "users")
public class User extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    // Domain behavior only
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
    
    public void updateProfile(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}

// 2. UserPrincipal.java (Security adapter)
public class UserPrincipal implements UserDetails {
    private final User user;
    
    public UserPrincipal(User user) {
        this.user = user;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
    
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true; // Or delegate to user.isActive() if added
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    // Expose user for accessing domain properties
    public User getUser() {
        return user;
    }
}

// 3. Update CustomUserDetailsService
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return new UserPrincipal(user);  // Wrap in adapter
    }
}
```

#### Benefits

-  User entity focuses on domain logic
-  Security concerns isolated in UserPrincipal
-  Easier to test domain logic without Spring Security
-  Can add account status fields without affecting UserDetails contract
-  Clear separation of concerns

#### Migration Impact

- **Breaking Change**: YES - All code using `User as UserDetails` must be updated
- **Affected Files**: 
  - `AuthenticationService.java` (casting to User)
  - `BlogPostService.java` (getCurrentUser method)
  - `TestController.java` (authentication.getPrincipal())
- **Test Impact**: Medium - Mock UserPrincipal instead of User
- **Database Impact**: None

---

###  HIGH: BlogPostService - Mixed Responsibilities

**File**: `src/main/java/com/harrish/auth/service/BlogPostService.java:22-140`  
**Severity**: HIGH  
**Impact**: Maintainability, Testability, Reusability

#### Issue Description

The `BlogPostService` class has multiple reasons to change:

1. **Business logic** for blog operations (lines 32-92)
2. **DTO mapping logic** (lines 94-117)
3. **Security/authorization logic** (lines 119-139)
4. **User retrieval** from security context (lines 119-125)

Changes to DTO structure, authorization rules, or business logic all require modifying this class.

```java
//  CURRENT: Service does too much
@Service
public class BlogPostService {
    public BlogPostResponse createBlogPost(CreateBlogPostRequest request) {
        // 1. Get current user (infrastructure concern)
        User currentUser = getCurrentUser();
        
        // 2. Create entity (business logic)
        BlogPost blogPost = BlogPost.builder()...build();
        
        // 3. Save (persistence)
        BlogPost saved = blogPostRepository.save(blogPost);
        
        // 4. Map to DTO (presentation concern)
        return BlogPostResponse.builder()
            .id(saved.getId())
            .title(saved.getTitle())
            // ... 15 lines of mapping
            .build();
    }
    
    // Mixes mapping logic
    private UserDto mapUserToDto(User user) { ... }
    
    // Mixes authorization logic
    private boolean isBlogPostCreator(Long blogPostId) { ... }
    
    // Mixes infrastructure access
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // ...
    }
}
```

#### Refactoring Recommendation

**Priority**: HIGH  
**Effort**: High (4-5 hours)

```java
//  REFACTORED: Split responsibilities

// 1. BlogPostService.java (pure business logic)
@Service
@RequiredArgsConstructor
public class BlogPostService {
    private final BlogPostRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final BlogPostAuthorizationService authorizationService;
    
    @Transactional
    public BlogPost createBlogPost(String title, String content) {
        User currentUser = currentUserProvider.getCurrentUser();
        
        BlogPost blogPost = BlogPost.builder()
            .title(title)
            .content(content)
            .build();
        
        return repository.save(blogPost);
    }
    
    @Transactional
    public BlogPost updateBlogPost(Long id, String title, String content) {
        authorizationService.requireBlogPostOwnership(id);
        
        BlogPost blogPost = repository.findById(id)
            .orElseThrow(BlogPostNotFoundException::new);
        
        blogPost.updateTitle(title);
        blogPost.updateContent(content);
        
        return repository.save(blogPost);
    }
    
    @Transactional(readOnly = true)
    public BlogPost getBlogPost(Long id) {
        return repository.findById(id)
            .orElseThrow(BlogPostNotFoundException::new);
    }
    
    @Transactional(readOnly = true)
    public List<BlogPost> getAllBlogPosts() {
        return repository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<BlogPost> getCurrentUserBlogPosts() {
        User currentUser = currentUserProvider.getCurrentUser();
        return repository.findByCreatedByOrderByCreatedAtDesc(currentUser);
    }
    
    @Transactional
    public void deleteBlogPost(Long id) {
        authorizationService.requireBlogPostOwnership(id);
        repository.deleteById(id);
    }
}

// 2. BlogPostMapper.java (mapping responsibility)
@Component
@RequiredArgsConstructor
public class BlogPostMapper {
    
    public BlogPostResponse toResponse(BlogPost entity) {
        return BlogPostResponse.builder()
            .id(entity.getId())
            .title(entity.getTitle())
            .content(entity.getContent())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .author(toUserDto(entity.getCreatedBy()))
            .build();
    }
    
    public List<BlogPostResponse> toResponseList(List<BlogPost> entities) {
        return entities.stream()
            .map(this::toResponse)
            .toList();
    }
    
    public UserDto toUserDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .role(user.getRole())
            .build();
    }
}

// 3. BlogPostAuthorizationService.java (authorization)
@Service
@RequiredArgsConstructor
public class BlogPostAuthorizationService {
    private final BlogPostRepository blogPostRepository;
    private final CurrentUserProvider currentUserProvider;
    
    public void requireBlogPostOwnership(Long blogPostId) {
        if (!isBlogPostOwner(blogPostId)) {
            throw new AccessDeniedException("You do not have permission to modify this blog post");
        }
    }
    
    public boolean isBlogPostOwner(Long blogPostId) {
        User currentUser = currentUserProvider.getCurrentUser();
        
        return blogPostRepository.findById(blogPostId)
            .map(BlogPost::getCreatedBy)
            .map(User::getId)
            .map(creatorId -> creatorId.equals(currentUser.getId()))
            .orElse(false);
    }
}

// 4. CurrentUserProvider.java (infrastructure abstraction)
public interface CurrentUserProvider {
    User getCurrentUser();
    Optional<User> getCurrentUserIfPresent();
}

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {
    private final UserRepository userRepository;
    
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("No authenticated user found");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("Current user not found"));
    }
    
    @Override
    public Optional<User> getCurrentUserIfPresent() {
        try {
            return Optional.of(getCurrentUser());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

// 5. BlogPostController.java (updated to use mapper)
@RestController
@RequestMapping("/api/v1/blog-posts")
@RequiredArgsConstructor
public class BlogPostController {
    private final BlogPostService blogPostService;
    private final BlogPostMapper blogPostMapper;
    
    @PostMapping
    public ResponseEntity<BlogPostResponse> createBlogPost(
            @Valid @RequestBody CreateBlogPostRequest request) {
        
        BlogPost blogPost = blogPostService.createBlogPost(
            request.getTitle(),
            request.getContent()
        );
        
        BlogPostResponse response = blogPostMapper.toResponse(blogPost);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BlogPostResponse> getBlogPost(@PathVariable Long id) {
        BlogPost blogPost = blogPostService.getBlogPost(id);
        BlogPostResponse response = blogPostMapper.toResponse(blogPost);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<BlogPostResponse>> getAllBlogPosts() {
        List<BlogPost> blogPosts = blogPostService.getAllBlogPosts();
        List<BlogPostResponse> responses = blogPostMapper.toResponseList(blogPosts);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/my-posts")
    public ResponseEntity<List<BlogPostResponse>> getMyBlogPosts() {
        List<BlogPost> blogPosts = blogPostService.getCurrentUserBlogPosts();
        List<BlogPostResponse> responses = blogPostMapper.toResponseList(blogPosts);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<BlogPostResponse> updateBlogPost(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBlogPostRequest request) {
        
        BlogPost updated = blogPostService.updateBlogPost(
            id,
            request.getTitle(),
            request.getContent()
        );
        
        BlogPostResponse response = blogPostMapper.toResponse(updated);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlogPost(@PathVariable Long id) {
        blogPostService.deleteBlogPost(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### Benefits

-  BlogPostService focuses only on business logic
-  Mapping logic reusable across controllers
-  Authorization logic reusable and testable
-  Easy to mock CurrentUserProvider in tests
-  Can swap SecurityContext implementation without changing business logic

#### Migration Impact

- **Breaking Change**: NO - Controllers updated to use mapper
- **New Files**: 
  - `BlogPostMapper.java`
  - `BlogPostAuthorizationService.java`
  - `CurrentUserProvider.java` (interface)
  - `SecurityContextCurrentUserProvider.java` (implementation)
- **Test Impact**: Medium - More focused unit tests possible
- **Database Impact**: None

---

###  MEDIUM: AuthenticationService - Mixed Business and Infrastructure

**File**: `src/main/java/com/harrish/auth/service/AuthenticationService.java:19-114`  
**Severity**: MEDIUM  
**Impact**: Maintainability

#### Issue Description

Combines multiple responsibilities:

1. User registration business logic (lines 37-58)
2. Authentication coordination (lines 60-83)
3. Token refresh logic (lines 85-113)
4. Token validation logic (lines 92-94, 101-102)

#### Refactoring Recommendation

**Priority**: MEDIUM  
**Effort**: Medium (3-4 hours)

```java
//  REFACTORED: Split into focused services

// 1. UserRegistrationService.java
@Service
@RequiredArgsConstructor
public class UserRegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public User register(String firstName, String lastName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already registered");
        }
        
        User user = User.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .password(passwordEncoder.encode(password))
            .role(Role.USER)
            .build();
        
        return userRepository.save(user);
    }
}

// 2. AuthenticationService.java (focused on authentication)
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    
    public AuthenticationResponse authenticate(String email, String password) {
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );
        
        // Generate tokens
        var userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        long expiresIn = jwtService.getAccessTokenExpirationMs();
        
        return new AuthenticationResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}

// 3. TokenService.java (token operations)
@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    
    public AuthenticationResponse refreshAccessToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        String email = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        if (!jwtService.isValid(refreshToken, userDetails)) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }
        
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        long expiresIn = jwtService.getAccessTokenExpirationMs();
        
        return new AuthenticationResponse(newAccessToken, newRefreshToken, "Bearer", expiresIn);
    }
    
    public void validateToken(String token) {
        // Validation logic
    }
}

// 4. AuthenticationController.java (updated)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final UserRegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final TokenService tokenService;
    
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(
            request.getFirstName(),
            request.getLastName(),
            request.getEmail(),
            request.getPassword()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RegisterResponse("User registered successfully"));
    }
    
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request) {
        
        AuthenticationResponse response = authenticationService.authenticate(
            request.getEmail(),
            request.getPassword()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        AuthenticationResponse response = tokenService.refreshAccessToken(
            request.getRefreshToken()
        );
        
        return ResponseEntity.ok(response);
    }
}
```

#### Benefits

-  Each service has one clear responsibility
-  Easier to test registration, authentication, and token operations separately
-  Can reuse registration logic (e.g., admin creating users)
-  Token operations centralized

#### Migration Impact

- **Breaking Change**: NO - Controllers updated
- **New Files**: `UserRegistrationService.java`, `TokenService.java`
- **Test Impact**: Low - Tests become more focused
- **Database Impact**: None

---

###  MEDIUM: JwtService - JWT and Validation Mixed

**File**: `src/main/java/com/harrish/auth/security/JwtService.java:19-103`  
**Severity**: MEDIUM  
**Impact**: Maintainability, Testability

#### Issue Description

Combines token generation, parsing, and validation:

1. Token generation (lines 44-69)
2. Token parsing/claims extraction (lines 35-96)
3. Token validation logic (lines 71-87)

#### Refactoring Recommendation

**Priority**: LOW  
**Effort**: Medium (2-3 hours)  
**Reason for LOW Priority**: Current implementation is manageable; focus on higher-priority SRP violations first.

```java
//  REFACTORED: Split JWT concerns

// 1. JwtTokenGenerator.java
@Component
@RequiredArgsConstructor
public class JwtTokenGenerator {
    private final JwtProperties jwtProperties;
    
    public String generateAccessToken(UserDetails user) {
        return generateToken(user, jwtProperties.getAccessTokenExpirationMs(), false);
    }
    
    public String generateRefreshToken(UserDetails user) {
        return generateToken(user, jwtProperties.getRefreshTokenExpirationMs(), true);
    }
    
    private String generateToken(UserDetails user, long expirationMs, boolean isRefreshToken) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", isRefreshToken ? "refresh" : "access");
        
        return Jwts.builder()
            .claims(claims)
            .subject(user.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

// 2. JwtTokenParser.java
@Component
@RequiredArgsConstructor
public class JwtTokenParser {
    private final JwtProperties jwtProperties;
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public boolean isRefreshToken(String token) {
        String type = extractClaim(token, claims -> claims.get("type", String.class));
        return "refresh".equals(type);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

// 3. JwtTokenValidator.java
@Component
@RequiredArgsConstructor
public class JwtTokenValidator {
    private final JwtTokenParser parser;
    
    public boolean isValid(String token, UserDetails user) {
        String username = parser.extractUsername(token);
        return username.equals(user.getUsername()) && !isExpired(token);
    }
    
    public boolean isExpired(String token) {
        Date expiration = parser.extractExpiration(token);
        return expiration.before(new Date());
    }
}

// 4. JwtService.java (facade for backward compatibility)
@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtTokenGenerator generator;
    private final JwtTokenParser parser;
    private final JwtTokenValidator validator;
    
    // Delegate to focused components
    public String generateAccessToken(UserDetails user) {
        return generator.generateAccessToken(user);
    }
    
    public String generateRefreshToken(UserDetails user) {
        return generator.generateRefreshToken(user);
    }
    
    public String extractUsername(String token) {
        return parser.extractUsername(token);
    }
    
    public boolean isRefreshToken(String token) {
        return parser.isRefreshToken(token);
    }
    
    public boolean isValid(String token, UserDetails user) {
        return validator.isValid(token, user);
    }
}
```

#### Benefits

-  Clear separation of token generation, parsing, and validation
-  Easier to test each concern independently
-  Can replace signing algorithm without affecting parsing
-  Facade pattern maintains backward compatibility

#### Migration Impact

- **Breaking Change**: NO - Facade maintains existing API
- **New Files**: 3 new JWT components
- **Test Impact**: Low - More focused unit tests
- **Database Impact**: None

---

###  MEDIUM: SecurityConfig - Multiple Configuration Concerns

**File**: `src/main/java/com/harrish/auth/security/SecurityConfig.java:24-100`  
**Severity**: MEDIUM  
**Impact**: Maintainability

#### Issue Description

Handles multiple security configuration aspects:

1. HTTP security configuration (lines 40-81)
2. Authentication provider setup (lines 83-89)
3. Password encoder bean (lines 91-94)
4. Authentication manager bean (lines 96-99)

#### Refactoring Recommendation

**Priority**: LOW  
**Effort**: Low (1-2 hours)

```java
//  REFACTORED: Split configuration concerns

// 1. HttpSecurityConfig.java
@Configuration
@RequiredArgsConstructor
public class HttpSecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/test/public").permitAll()
                .requestMatchers("/v1/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data:; " +
                    "font-src 'self'; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'"
                ))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .permissionsPolicy(permissions -> permissions.policy(
                    "geolocation=(), microphone=(), camera=()"
                ))
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

// 2. AuthenticationConfig.java
@Configuration
@RequiredArgsConstructor
public class AuthenticationConfig {
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}

// 3. PasswordEncoderConfig.java
@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### Benefits

-  Each configuration class has a focused responsibility
-  Easier to find and modify specific configuration
-  Can disable/enable features independently

#### Migration Impact

- **Breaking Change**: NO
- **Test Impact**: None
- **Database Impact**: None

---

###  LOW: GlobalExceptionHandler - Multiple Exception Types

**File**: `src/main/java/com/harrish/auth/exception/GlobalExceptionHandler.java:23-159`  
**Severity**: LOW  
**Impact**: Maintainability

#### Issue Description

While centralized exception handling is a pattern, this class handles 7 different exception types with different mapping logic. Changes to any exception type's handling require modifying this class.

#### Refactoring Recommendation

**Priority**: LOW  
**Effort**: Medium (3-4 hours)  
**Note**: This is acceptable for now; consider refactoring only if exception types grow beyond 10-12.

```java
//  REFACTORED: Strategy pattern for exception mapping

// 1. ExceptionMapper interface
public interface ExceptionMapper<T extends Exception> {
    ProblemDetail toProblemDetail(T exception, HttpServletRequest request);
    boolean supports(Class<? extends Exception> exceptionClass);
}

// 2. Base abstract mapper
public abstract class AbstractExceptionMapper<T extends Exception> implements ExceptionMapper<T> {
    
    @Override
    public boolean supports(Class<? extends Exception> exceptionClass) {
        return getSupportedExceptionClass().isAssignableFrom(exceptionClass);
    }
    
    protected abstract Class<T> getSupportedExceptionClass();
    
    protected ProblemDetail createProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }
}

// 3. Specific mappers
@Component
public class NotFoundExceptionMapper extends AbstractExceptionMapper<RuntimeException> {
    
    @Override
    protected Class<RuntimeException> getSupportedExceptionClass() {
        return RuntimeException.class;
    }
    
    @Override
    public boolean supports(Class<? extends Exception> exceptionClass) {
        return UserNotFoundException.class.isAssignableFrom(exceptionClass) ||
               BlogPostNotFoundException.class.isAssignableFrom(exceptionClass);
    }
    
    @Override
    public ProblemDetail toProblemDetail(RuntimeException ex, HttpServletRequest request) {
        return createProblemDetail(
            HttpStatus.NOT_FOUND,
            "Resource Not Found",
            ex.getMessage(),
            request
        );
    }
}

@Component
public class EmailAlreadyExistsExceptionMapper 
        extends AbstractExceptionMapper<EmailAlreadyExistsException> {
    
    @Override
    protected Class<EmailAlreadyExistsException> getSupportedExceptionClass() {
        return EmailAlreadyExistsException.class;
    }
    
    @Override
    public ProblemDetail toProblemDetail(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.CONFLICT,
            "Email Already Exists",
            ex.getMessage(),
            request
        );
        problemDetail.setProperty("field", "email");
        return problemDetail;
    }
}

// 4. Registry-based handler
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final List<ExceptionMapper<?>> exceptionMappers;
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(
            Exception ex,
            HttpServletRequest request) {
        
        // Find matching mapper
        ExceptionMapper mapper = exceptionMappers.stream()
            .filter(m -> m.supports(ex.getClass()))
            .findFirst()
            .orElse(new DefaultExceptionMapper());
        
        @SuppressWarnings("unchecked")
        ProblemDetail problemDetail = mapper.toProblemDetail(ex, request);
        
        return ResponseEntity
            .status(problemDetail.getStatus())
            .body(problemDetail);
    }
}
```

#### Benefits

-  Open for extension - add new exception types without modifying handler
-  Each mapper focuses on one exception type
-  Easier to test exception mapping logic

#### Migration Impact

- **Breaking Change**: NO
- **New Files**: Multiple mapper classes
- **Test Impact**: Low - More focused tests
- **Database Impact**: None

---

## 2. Open/Closed Principle (OCP) Violations

> "Software entities should be open for extension, but closed for modification."

###  HIGH: GlobalExceptionHandler - Type-Based Exception Handling

**File**: `src/main/java/com/harrish/auth/exception/GlobalExceptionHandler.java:32-158`  
**Severity**: HIGH  
**Impact**: Maintainability, Extensibility

#### Issue Description

Every new exception type requires adding a new handler method. The class must be modified to handle new exceptions:

- Lines 32-52: NotFound exceptions
- Lines 54-66: EmailAlreadyExists
- Lines 68-81: InvalidToken
- Lines 83-95: BadCredentials
- Lines 97-109: AccessDenied
- Lines 111-124: IllegalArgument
- Lines 126-141: MethodArgumentNotValid
- Lines 143-158: Generic Exception

Adding custom business exceptions (e.g., `InsufficientBalanceException`, `DuplicatePostException`) requires modifying this class each time.

#### Refactoring Recommendation

See SRP VIOLATION 1.6 for detailed refactoring with strategy pattern.

**Priority**: MEDIUM  
**Effort**: Medium (3-4 hours)

---

###  MEDIUM: SecurityConfig - Hardcoded Endpoint Patterns

**File**: `src/main/java/com/harrish/auth/security/SecurityConfig.java:44-53`  
**Severity**: MEDIUM  
**Impact**: Maintainability, Configuration Management

#### Issue Description

Security rules are hardcoded. Adding new public/protected endpoints requires modifying this class:

```java
.requestMatchers("/api/v1/auth/**").permitAll()
.requestMatchers("/api/v1/test/public").permitAll()
.requestMatchers("/v1/api-docs/**").permitAll()
```

#### Refactoring Recommendation

**Priority**: LOW  
**Effort**: Medium (2-3 hours)

```java
//  REFACTORED: Configurable security rules

// 1. application.yml
security:
  rules:
    - pattern: "/api/v1/auth/**"
      access: "permitAll"
    - pattern: "/api/v1/test/public"
      access: "permitAll"
    - pattern: "/v1/api-docs/**"
      access: "permitAll"
    - pattern: "/swagger-ui/**"
      access: "permitAll"
    - pattern: "/actuator/health"
      access: "permitAll"
    - pattern: "/api/v1/admin/**"
      access: "hasRole('ADMIN')"

// 2. SecurityProperties.java
@ConfigurationProperties(prefix = "security")
@Validated
public class SecurityProperties {
    
    @NotEmpty
    private List<SecurityRule> rules = new ArrayList<>();
    
    public List<SecurityRule> getRules() {
        return rules;
    }
    
    public void setRules(List<SecurityRule> rules) {
        this.rules = rules;
    }
    
    public static class SecurityRule {
        @NotBlank
        private String pattern;
        
        @NotBlank
        private String access; // "permitAll", "authenticated", "hasRole(ADMIN)"
        
        private int order = 0;
        
        // Getters and setters
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public String getAccess() { return access; }
        public void setAccess(String access) { this.access = access; }
        
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }
}

// 3. SecurityConfig.java (updated)
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {
    private final SecurityProperties securityProperties;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // Apply rules from configuration
                securityProperties.getRules().stream()
                    .sorted(Comparator.comparingInt(SecurityProperties.SecurityRule::getOrder))
                    .forEach(rule -> applyRule(auth, rule));
                
                // Default: all other requests require authentication
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    private void applyRule(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            SecurityProperties.SecurityRule rule) {
        
        switch (rule.getAccess().toLowerCase()) {
            case "permitall" -> auth.requestMatchers(rule.getPattern()).permitAll();
            case "authenticated" -> auth.requestMatchers(rule.getPattern()).authenticated();
            default -> {
                // Support SpEL expressions like "hasRole('ADMIN')"
                if (rule.getAccess().startsWith("hasRole")) {
                    String role = rule.getAccess().replaceAll("hasRole\\('(.+)'\\)", "$1");
                    auth.requestMatchers(rule.getPattern()).hasRole(role);
                } else if (rule.getAccess().startsWith("hasAuthority")) {
                    String authority = rule.getAccess().replaceAll("hasAuthority\\('(.+)'\\)", "$1");
                    auth.requestMatchers(rule.getPattern()).hasAuthority(authority);
                }
            }
        }
    }
}
```

#### Benefits

-  Add new endpoints without modifying code
-  Different rules per environment (dev, prod)
-  Centralized security rule management
-  Easier to audit security configuration

#### Migration Impact

- **Breaking Change**: NO
- **Configuration Change**: YES - Move rules to application.yml
- **Test Impact**: Low
- **Database Impact**: None

---

###  MEDIUM: Role-Based Authorization - Hardcoded String

**File**: `src/main/java/com/harrish/auth/model/User.java:45`  
**Severity**: MEDIUM  
**Impact**: Flexibility, Extensibility

#### Issue Description

Authority creation is hardcoded with "ROLE_" prefix concatenation:

```java
return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
```

Adding new authority types or changing the authority structure requires modifying this code.

#### Refactoring Recommendation

**Priority**: LOW (unless role system needs to be extended)  
**Effort**: Medium (2-3 hours)

```java
//  REFACTORED: Authority provider strategy

// 1. AuthorityProvider interface
public interface AuthorityProvider {
    Collection<? extends GrantedAuthority> getAuthorities(User user);
}

// 2. RoleBasedAuthorityProvider
@Component
public class RoleBasedAuthorityProvider implements AuthorityProvider {
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
}

// 3. HierarchicalAuthorityProvider (alternative)
@Component
@ConditionalOnProperty(name = "security.authority-provider", havingValue = "hierarchical")
public class HierarchicalAuthorityProvider implements AuthorityProvider {
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // Add role
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        
        // Add hierarchical permissions
        if (user.getRole() == Role.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("PERMISSION_MANAGE_USERS"));
            authorities.add(new SimpleGrantedAuthority("PERMISSION_DELETE_ANY_POST"));
        }
        
        return authorities;
    }
}

// 4. UserPrincipal (updated to use provider)
public class UserPrincipal implements UserDetails {
    private final User user;
    private final AuthorityProvider authorityProvider;
    
    public UserPrincipal(User user, AuthorityProvider authorityProvider) {
        this.user = user;
        this.authorityProvider = authorityProvider;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorityProvider.getAuthorities(user);
    }
}
```

#### Benefits

-  Can switch authority strategies without modifying User
-  Support for hierarchical roles
-  Support for permission-based access control

#### Migration Impact

- **Breaking Change**: NO (if using UserPrincipal adapter)
- **Configuration**: Add provider selection property
- **Test Impact**: Low
- **Database Impact**: None

---

###  LOW: Role Enum - Closed for Extension

**File**: `src/main/java/com/harrish/auth/model/Role.java:3-6`  
**Severity**: LOW  
**Impact**: Extensibility (only if dynamic roles needed)

#### Issue Description

Using enum for roles makes it closed for extension:

```java
public enum Role {
    USER,
    ADMIN
}
```

Adding new roles requires modifying the source code and recompilation. If the system grows to support dynamic roles (MODERATOR, EDITOR, VIEWER, etc.), the enum approach becomes problematic.

#### Refactoring Recommendation

**Priority**: LOW (only if dynamic roles are needed)  
**Effort**: High (5-6 hours)  
**Note**: Enum approach is acceptable for fixed, compile-time roles. Only refactor if business requires runtime role management.

```java
//  REFACTORED: Entity-based roles (only if needed)

// 1. Role entity
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
    
    // Getters, setters, equals, hashCode
}

// 2. Permission entity
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    // Getters, setters, equals, hashCode
}

// 3. User entity (updated)
@Entity
@Table(name = "users")
public class User extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;  // Now an entity instead of enum
    
    // Getters and setters
}

// 4. RoleRepository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}

// 5. Data initialization
@Component
public class RoleDataInitializer implements ApplicationRunner {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleRepository.count() == 0) {
            // Create default permissions
            Permission readPosts = createPermission("READ_POSTS", "Can read blog posts");
            Permission writePosts = createPermission("WRITE_POSTS", "Can create/edit own posts");
            Permission deleteAnyPost = createPermission("DELETE_ANY_POST", "Can delete any post");
            Permission manageUsers = createPermission("MANAGE_USERS", "Can manage users");
            
            // Create default roles
            Role userRole = new Role();
            userRole.setName("USER");
            userRole.setDescription("Regular user");
            userRole.setPermissions(Set.of(readPosts, writePosts));
            roleRepository.save(userRole);
            
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Administrator");
            adminRole.setPermissions(Set.of(readPosts, writePosts, deleteAnyPost, manageUsers));
            roleRepository.save(adminRole);
        }
    }
}
```

#### Benefits

-  Add new roles at runtime without code changes
-  Fine-grained permission control
-  Role management UI possible

#### Migration Impact

- **Breaking Change**: YES - Major schema change
- **Database Migration**: Create roles, permissions, role_permissions tables
- **Data Migration**: Convert enum values to entities
- **Test Impact**: High - Update all role-related tests
- **Recommendation**: Only do this if business requires dynamic role management

---

## 3. Liskov Substitution Principle (LSP) Violations

> "Subtypes must be substitutable for their base types."

###  MEDIUM: User Implementing UserDetails - Contract Violation Risk

**File**: `src/main/java/com/harrish/auth/model/User.java:54-72`  
**Severity**: MEDIUM  
**Impact**: Correctness, Maintainability

#### Issue Description

The `User` entity implements Spring Security's `UserDetails` interface, but all account status methods return hardcoded `true`:

```java
public boolean isAccountNonExpired() { return true; }
public boolean isAccountNonLocked() { return true; }
public boolean isCredentialsNonExpired() { return true; }
public boolean isEnabled() { return true; }
```

This violates LSP because:

1. The contract of `UserDetails` expects these methods to reflect actual account status
2. Substituting this `User` with another `UserDetails` implementation that properly checks status could break functionality
3. If you later need to add account status fields (enabled, locked, expired), you'll need to modify the entity AND the methods, risking breaking existing code that assumes these are always true

#### Refactoring Recommendation

See SRP VIOLATION 1.1 for detailed refactoring using composition (UserPrincipal adapter).

**Priority**: HIGH  
**Effort**: Medium (2-3 hours)

---

###  LOW: BlogPost Extending Auditable - Inheritance Misuse

**File**: `src/main/java/com/harrish/auth/model/BlogPost.java:17`  
**Severity**: LOW  
**Impact**: Design Quality

#### Issue Description

While not a direct LSP violation, using inheritance for `Auditable` creates tight coupling. The relationship is "has-a" (a BlogPost has audit fields) rather than "is-a" (a BlogPost is an Auditable).

This can lead to LSP issues:

- `Auditable` exposes audit fields through getters that shouldn't be modified directly
- Any subclass of `Auditable` inherits all fields, which may not make sense for all entity types
- You cannot substitute `BlogPost` for `Auditable` in a meaningful way

#### Refactoring Recommendation

**Priority**: LOW  
**Effort**: Medium (2-3 hours)

```java
//  REFACTORED: Use composition with embedded type

// 1. AuditMetadata.java
@Embeddable
public class AuditMetadata {
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false, updatable = false)
    private User createdBy;
    
    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id", nullable = false)
    private User updatedBy;
    
    // Getters only (immutable after creation)
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public User getCreatedBy() { return createdBy; }
    public User getUpdatedBy() { return updatedBy; }
}

// 2. BlogPost.java (updated)
@Entity
@Table(name = "blog_posts")
public class BlogPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, length = 10000)
    private String content;
    
    @Embedded
    private AuditMetadata audit;
    
    // Delegate audit methods
    public LocalDateTime getCreatedAt() {
        return audit != null ? audit.getCreatedAt() : null;
    }
    
    public LocalDateTime getUpdatedAt() {
        return audit != null ? audit.getUpdatedAt() : null;
    }
    
    public User getCreatedBy() {
        return audit != null ? audit.getCreatedBy() : null;
    }
    
    public User getUpdatedBy() {
        return audit != null ? audit.getUpdatedBy() : null;
    }
    
    // Domain methods
    public void updateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        this.title = title;
    }
    
    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be blank");
        }
        this.content = content;
    }
}

// 3. User.java (updated)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role;
    
    @Embedded
    private AuditMetadata audit;  // Add if User needs auditing
    
    // No need to extend Auditable
}
```

#### Benefits

-  Composition over inheritance (best practice)
-  Audit metadata encapsulated and immutable
-  Can add auditing to any entity without inheritance
-  Clear "has-a" relationship

#### Migration Impact

- **Breaking Change**: YES - Existing code accessing audit fields must be updated
- **Database Impact**: None (same column names with @Embedded)
- **Test Impact**: Low - Update tests accessing audit fields
- **Effort**: Medium (need to update all entities extending Auditable)

---

## 4. Interface Segregation Principle (ISP) Violations

> "Clients should not be forced to depend on interfaces they do not use."

###  MEDIUM: UserDetails Interface Forcing Unnecessary Methods

**File**: `src/main/java/com/harrish/auth/model/User.java:43-72`  
**Severity**: MEDIUM  
**Impact**: Design Quality, Clarity

#### Issue Description

The `UserDetails` interface from Spring Security forces the `User` entity to implement 7 methods, but only 2 are actually meaningful in this context:

-  `getAuthorities()` - needed for authorization
-  `getPassword()` - needed for authentication

The other 5 methods are dummy implementations that return hardcoded values:

- `getUsername()` - delegates to email
- `isAccountNonExpired()` - always true
- `isAccountNonLocked()` - always true
- `isCredentialsNonExpired()` - always true
- `isEnabled()` - always true

This violates ISP because the client (User) is forced to depend on methods it doesn't use.

#### Refactoring Recommendation

See SRP VIOLATION 1.1 for detailed refactoring using UserPrincipal adapter.

**Priority**: HIGH  
**Effort**: Medium (2-3 hours)

---

###  LOW: JpaRepository Interface - Unused Methods

**File**: 
- `src/main/java/com/harrish/auth/repository/UserRepository.java:8`
- `src/main/java/com/harrish/auth/repository/BlogPostRepository.java:9`

**Severity**: LOW  
**Impact**: API Surface, Potential Misuse

#### Issue Description

Both repositories extend `JpaRepository<T, ID>` which provides 17+ CRUD methods, but the services likely don't use all of them:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Exposes unused methods like:
- `saveAll()` - bulk operations not used
- `deleteAll()` - dangerous, not used
- `deleteAllInBatch()` - not used
- `getOne()` - deprecated, not used
- `flush()` - manual flush, not used with @Transactional

#### Refactoring Recommendation

**Priority**: LOW  
**Effort**: Low (1-2 hours)  
**Note**: Only refactor if strict API control is needed

```java
//  REFACTORED: Custom base repository with only needed methods

// 1. ReadOnlyRepository.java
@NoRepositoryBean
public interface ReadOnlyRepository<T, ID> extends Repository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    Page<T> findAll(Pageable pageable);
    boolean existsById(ID id);
    long count();
}

// 2. CrudRepository.java
@NoRepositoryBean
public interface CrudRepository<T, ID> extends ReadOnlyRepository<T, ID> {
    <S extends T> S save(S entity);
    void deleteById(ID id);
    void delete(T entity);
}

// 3. UserRepository.java (updated)
public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

// 4. BlogPostRepository.java (updated)
public interface BlogPostRepository extends CrudRepository<BlogPost, Long> {
    List<BlogPost> findByCreatedByOrderByCreatedAtDesc(User user);
}
```

#### Benefits

-  Minimal API surface - only expose needed methods
-  Prevents accidental use of dangerous methods (deleteAll)
-  Clear intent of read-only vs read-write repositories

#### Migration Impact

- **Breaking Change**: NO (methods are compatible)
- **Test Impact**: None
- **Database Impact**: None

---

## 5. Dependency Inversion Principle (DIP) Violations

> "High-level modules should not depend on low-level modules. Both should depend on abstractions."

###  MEDIUM: BlogPostService Direct Dependency on SecurityContext

**File**: `src/main/java/com/harrish/auth/service/BlogPostService.java:119-125`  
**Severity**: MEDIUM  
**Impact**: Testability, Coupling

#### Issue Description

The service directly accesses `SecurityContextHolder.getContext()`:

```java
private User getCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var email = authentication.getName();
    return userRepository.findByEmail(email).orElseThrow(UserNotFoundException::new);
}
```

This creates tight coupling to Spring Security's infrastructure. The high-level business logic (BlogPostService) depends on low-level infrastructure (SecurityContextHolder).

#### Refactoring Recommendation

See SRP VIOLATION 1.2 for detailed refactoring with `CurrentUserProvider` abstraction.

**Priority**: HIGH  
**Effort**: Low (1-2 hours)

---

###  MEDIUM: JpaAuditingConfig Direct SecurityContext Access

**File**: `src/main/java/com/harrish/auth/config/JpaAuditingConfig.java:17-32`  
**Severity**: MEDIUM  
**Impact**: Testability, Coupling

#### Issue Description

The auditing configuration directly accesses `SecurityContextHolder`:

```java
@Bean
public AuditorAware<User> auditorProvider() {
    return () -> {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // ...
    };
}
```

#### Refactoring Recommendation

**Priority**: MEDIUM  
**Effort**: Low (30 minutes)

```java
//  REFACTORED: Use CurrentUserProvider abstraction

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class JpaAuditingConfig {
    
    @Bean
    public AuditorAware<User> auditorProvider(CurrentUserProvider currentUserProvider) {
        return () -> {
            try {
                return Optional.of(currentUserProvider.getCurrentUser());
            } catch (Exception e) {
                // Return empty if no authenticated user (e.g., during system operations)
                return Optional.empty();
            }
        };
    }
}
```

#### Benefits

-  No direct dependency on SecurityContextHolder
-  Easy to test with mock CurrentUserProvider
-  Can swap security implementation

#### Migration Impact

- **Breaking Change**: NO
- **Dependency**: Requires CurrentUserProvider interface
- **Test Impact**: Low
- **Database Impact**: None

---

###  MEDIUM: TestController Direct SecurityContext Access

**File**: `src/main/java/com/harrish/auth/controller/TestController.java:51`  
**Severity**: LOW  
**Impact**: Coupling

#### Issue Description

Controller directly accesses `SecurityContextHolder`:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
```

#### Refactoring Recommendation

**Priority**: LOW  
**Effort**: Very Low (10 minutes)

```java
//  REFACTORED: Use method parameter injection

@GetMapping("/protected")
public ResponseEntity<UserInfoResponse> protectedEndpoint(
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
    
    User user = userPrincipal.getUser();
    
    return ResponseEntity.ok(new UserInfoResponse(
        "This is a protected endpoint. You are authenticated!",
        user.getEmail(),
        userPrincipal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
    ));
}
```

#### Benefits

-  No direct SecurityContext access
-  Easier to test (mock UserPrincipal)
-  Clearer intent

#### Migration Impact

- **Breaking Change**: NO
- **Test Impact**: Low
- **Database Impact**: None

---

###  LOW: Direct Dependency on BCryptPasswordEncoder

**File**: `src/main/java/com/harrish/auth/security/SecurityConfig.java:91-94`  
**Severity**: LOW  
**Impact**: Flexibility

#### Issue Description

The configuration directly instantiates `BCryptPasswordEncoder`:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

While this returns the `PasswordEncoder` interface, switching to a different encoder requires modifying this configuration.

#### Refactoring Recommendation

**Priority**: LOW  
**Effort**: Low (1-2 hours)  
**Note**: Only needed if encoder flexibility is required

```java
//  REFACTORED: Configurable encoder selection

// 1. application.yml
security:
  password:
    encoder: bcrypt  # or: argon2, pbkdf2, scrypt
    strength: 10

// 2. PasswordEncoderProperties.java
@ConfigurationProperties(prefix = "security.password")
@Validated
public class PasswordEncoderProperties {
    
    @NotNull
    private EncoderType encoder = EncoderType.BCRYPT;
    
    private int strength = 10;
    
    public enum EncoderType {
        BCRYPT, ARGON2, PBKDF2, SCRYPT
    }
    
    // Getters and setters
}

// 3. PasswordEncoderConfig.java
@Configuration
@EnableConfigurationProperties(PasswordEncoderProperties.class)
@RequiredArgsConstructor
public class PasswordEncoderConfig {
    private final PasswordEncoderProperties properties;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return switch (properties.getEncoder()) {
            case BCRYPT -> new BCryptPasswordEncoder(properties.getStrength());
            case ARGON2 -> Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            case PBKDF2 -> Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            case SCRYPT -> SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
        };
    }
}
```

#### Benefits

-  Switch encoders via configuration
-  No code changes for encoder selection
-  Different encoders per environment

#### Migration Impact

- **Breaking Change**: NO
- **Configuration**: Add encoder selection to application.yml
- **Test Impact**: None
- **Database Impact**: None (existing passwords still work)

---

###  LOW: Direct Instantiation of Response DTOs

**File**: Multiple files
- `src/main/java/com/harrish/auth/service/AuthenticationService.java:57,82,109`
- `src/main/java/com/harrish/auth/service/BlogPostService.java:95-103,111-116`

**Severity**: LOW  
**Impact**: Coupling

#### Issue Description

Services directly instantiate DTO objects using `new` keyword:

```java
return new RegisterResponse("User registered successfully");
return new AuthenticationResponse(accessToken, refreshToken, "Bearer", expiresIn);
return BlogPostResponse.builder()...build();
```

While DTOs are data structures, the mapping logic is embedded in the service layer, creating coupling between business logic and API representation.

#### Refactoring Recommendation

See SRP VIOLATION 1.2 for detailed refactoring with mapper components.

**Priority**: MEDIUM (covered in SRP refactoring)  
**Effort**: Already included in SRP refactoring

---

## Positive Patterns Found

The codebase demonstrates several good practices:

###  Dependency Injection

All services, controllers, and configurations use constructor-based dependency injection:

```java
@Service
@RequiredArgsConstructor
public class BlogPostService {
    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
}
```

###  Interface-Based Repositories

Repositories extend Spring Data JPA interfaces:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

###  Centralized Exception Handling

Global exception handler provides consistent error responses:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFoundException(...) {
        // ...
    }
}
```

###  Configuration Externalization

JWT properties externalized to configuration:

```java
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretKey;
    private long accessTokenExpirationMs;
    private long refreshTokenExpirationMs;
}
```

###  Transaction Management

Appropriate use of `@Transactional`:

```java
@Transactional
public BlogPost createBlogPost(CreateBlogPostRequest request) {
    // ...
}

@Transactional(readOnly = true)
public BlogPost getBlogPost(Long id) {
    // ...
}
```

---

## Recommended Refactoring Roadmap

### Phase 1: High-Priority Refactorings (Critical for Maintainability)

**Estimated Effort**: 8-10 hours  
**Impact**: High

1. **Separate User entity from UserDetails** (SRP 1.1, ISP 4.1, LSP 3.1)
   - Create `UserPrincipal` adapter
   - Update `CustomUserDetailsService`
   - Update authentication-related code
   - **Files**: User.java, CustomUserDetailsService.java, AuthenticationService.java, BlogPostService.java, TestController.java

2. **Split BlogPostService responsibilities** (SRP 1.2, DIP 5.1, 5.4)
   - Create `BlogPostMapper` component
   - Create `BlogPostAuthorizationService`
   - Create `CurrentUserProvider` interface and implementation
   - Update `BlogPostController`
   - **Files**: BlogPostService.java, BlogPostController.java, + 3 new files

3. **Update JpaAuditingConfig to use CurrentUserProvider** (DIP 5.2)
   - Inject `CurrentUserProvider` into auditor provider
   - **Files**: JpaAuditingConfig.java

### Phase 2: Medium-Priority Refactorings (Improves Extensibility)

**Estimated Effort**: 7-9 hours  
**Impact**: Medium

4. **Refactor AuthenticationService** (SRP 1.3)
   - Create `UserRegistrationService`
   - Create `TokenService`
   - Update `AuthenticationController`
   - **Files**: AuthenticationService.java, AuthenticationController.java, + 2 new files

5. **Make SecurityConfig configurable** (OCP 2.2)
   - Create `SecurityProperties` with security rules
   - Update `SecurityConfig` to apply rules from configuration
   - Move endpoint patterns to application.yml
   - **Files**: SecurityConfig.java, application.yml, + 1 new file

6. **Split SecurityConfig** (SRP 1.5)
   - Create `HttpSecurityConfig`
   - Create `AuthenticationConfig`
   - Create `PasswordEncoderConfig`
   - **Files**: SecurityConfig.java → 3 separate configs

### Phase 3: Low-Priority Refactorings (Nice to Have)

**Estimated Effort**: 6-8 hours  
**Impact**: Low-Medium

7. **Split JwtService** (SRP 1.4)
   - Create `JwtTokenGenerator`, `JwtTokenParser`, `JwtTokenValidator`
   - Keep `JwtService` as facade
   - **Files**: JwtService.java → 4 files

8. **Refactor GlobalExceptionHandler** (SRP 1.6, OCP 2.1)
   - Create exception mapper strategy pattern
   - Create individual exception mappers
   - Update `GlobalExceptionHandler` to use registry
   - **Files**: GlobalExceptionHandler.java, + multiple mapper files

9. **Use composition for Auditable** (LSP 3.2)
   - Create `AuditMetadata` embeddable
   - Update `User` and `BlogPost` to use composition
   - Remove `Auditable` abstract class
   - **Files**: Auditable.java, User.java, BlogPost.java, + 1 new file

10. **Refactor TestController** (DIP 5.3)
    - Use `@AuthenticationPrincipal` method parameter
    - **Files**: TestController.java

### Optional: Advanced Refactorings (Only If Needed)

**Estimated Effort**: 10-15 hours  
**Impact**: Depends on business requirements

11. **Make PasswordEncoder configurable** (DIP 5.5)
    - Only if encoder flexibility is required
    - **Files**: SecurityConfig.java, application.yml, + 1 new file

12. **Entity-based roles** (OCP 2.4)
    - Only if dynamic role management is required
    - **Files**: Role.java, User.java, + multiple new files + database migration

13. **Custom base repositories** (ISP 4.2)
    - Only if strict API control is needed
    - **Files**: UserRepository.java, BlogPostRepository.java, + 2 new base interfaces

---

## Implementation Guidelines

### Before Refactoring

1.  **Ensure all existing tests pass**
2.  **Create comprehensive tests** for areas being refactored
3.  **Commit current working state** to version control
4.  **Create feature branch** for refactoring

### During Refactoring

1. **Refactor one principle at a time** - Don't mix SRP and DIP changes in the same commit
2. **Keep commits small and focused** - Each commit should pass all tests
3. **Run tests after each change** - Catch issues early
4. **Update documentation** - Keep docs in sync with code

### After Refactoring

1.  **Verify all tests pass**
2.  **Test manually** - Ensure functionality works end-to-end
3.  **Review code changes** - Self-review before PR
4.  **Update architecture documentation** - Reflect new structure

### Testing Strategy

For each refactoring:

1. **Unit Tests**
   - Test new components in isolation
   - Mock dependencies
   - Focus on business logic

2. **Integration Tests**
   - Test component interactions
   - Use Spring Boot test slices (@WebMvcTest, @DataJpaTest)
   - Verify wiring is correct

3. **End-to-End Tests**
   - Test complete flows (register → login → create post → etc.)
   - Use @SpringBootTest
   - Verify nothing broke

---

## Summary

### Current State

- **Overall SOLID Score**: 6.8/10  Needs Improvement
- **Total Violations**: 19 (4 High, 10 Medium, 5 Low)
- **Critical Issues**: User/UserDetails mixing, BlogPostService doing too much, direct SecurityContext access

### Recommended Actions

1. **Immediate (Phase 1)**: Fix high-priority SRP and DIP violations
   - Separate User from UserDetails
   - Split BlogPostService
   - Abstract SecurityContext access

2. **Short-term (Phase 2)**: Improve extensibility
   - Refactor AuthenticationService
   - Make SecurityConfig configurable
   - Split configuration classes

3. **Long-term (Phase 3)**: Polish and optimize
   - Split JwtService
   - Refactor exception handling
   - Use composition over inheritance

### Expected Outcomes

After implementing Phase 1 refactorings:

- **SOLID Score**: ~8.0/10  Good
- **Maintainability**: Significantly improved
- **Testability**: Each component easily testable in isolation
- **Flexibility**: Can swap implementations without changing business logic
- **Code Quality**: Clearer responsibilities, better separation of concerns

---

## Related Reports

- **CODE_REVIEW_REPORT.md** - Detailed code review (Phase 2.1)
- **ARCHITECTURE_REVIEW.md** - High-level architecture assessment (Phase 1.3)
- **SECURITY_AUDIT_REPORT.md** - Security analysis (Phase 1.1)

---

## Next Steps

1. **Review this report** with the team
2. **Prioritize refactorings** based on business needs
3. **Create tasks** for Phase 1 refactorings
4. **Implement refactorings** systematically
5. **Update tests** to cover new structure
6. **Document changes** in architecture review

---

**End of SOLID Principles Report**
