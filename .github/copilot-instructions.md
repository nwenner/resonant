# Project Context

## Overview

AWS tag compliance platform for monitoring and enforcing tagging policies across AWS resources.
Spring Boot backend with React frontend, JWT authentication, PostgreSQL database.

## Architecture

- **Frontend**: React 18 + TypeScript + Vite
- **Backend**: Spring Boot 3.2.0 (Java 21) + JPA/Hibernate
- **Database**: PostgreSQL (production), H2 (tests)
- **State**: Zustand (auth/theme), TanStack Query (server state)
- **Build**: Gradle (backend), Vite (frontend)
- **Auth**: JWT tokens (24hr), stored in localStorage
- **AWS**: SDK v2, IAM role assumption with external ID

## Code Standards

### Backend (Java)

- Use Lombok annotations: `@Data`, `@RequiredArgsConstructor`, `@Slf4j`
- All entities must be user-owned with `@ManyToOne User` relationship
- Controllers use `@AuthenticationPrincipal User user` for authentication
- Services accept `UUID userId`, not User entities
- DTOs have static `fromEntity()` factory methods
- Use `@JdbcTypeCode(SqlTypes.JSON)` for Map/List columns
- Flyway migrations only - never use `ddl-auto: update` in production
- Unit tests with Mockito, integration tests with `@SpringBootTest`
- No MockMvc in controller tests
- Use parameterized logging: `log.info("Message {}", value)`

### Frontend (TypeScript)

- Use functional components with TypeScript strict mode
- TanStack Query for all data fetching (no manual fetch in components)
- Always define response types, never use `any`
- Custom hooks prefixed with `use`
- Axios interceptor handles 401 redirects (except `/auth/*`)
- Clean up intervals/subscriptions in `useEffect` return
- Status badges match backend enums exactly (OPEN, RUNNING, etc.)

## Naming Conventions

- **Java Classes**: PascalCase (`TagPolicyService`, `AwsAccount`)
- **Java Methods**: camelCase (`createPolicy`, `findByUserId`)
- **React Components**: PascalCase (`StatusBadge`, `ScanButton`)
- **Hooks**: camelCase with `use` prefix (`useScanJob`, `useViolations`)
- **Types/Interfaces**: PascalCase (`ScanJob`, `ComplianceViolation`)
- **API Endpoints**: kebab-case (`/api/tag-policies`, `/api/aws-accounts`)
- **Database Tables**: snake_case (`tag_policies`, `compliance_violations`)
- **AWS Resource Types**: `service:type` format (`s3:bucket`, `ec2:instance`)

## Project-Specific Patterns

### Entity Ownership (CRITICAL)

ALL entities must have user ownership verification:

```java

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

### TagPolicy Required Tags Semantics

```java
Map<String, List<String>> requiredTags;
// null list = any value allowed for tag key
// empty list = INVALID, don't use
// populated list = must match one of these values
```

### Compliance Violation Auto-Resolution

`ComplianceEvaluationService` automatically manages status:

- Creates violations as OPEN
- OPEN → RESOLVED when resource becomes compliant
- RESOLVED → OPEN if non-compliant again
- **NEVER modifies IGNORED status** (user-managed)
- Unique constraint: `(resource_id, policy_id)`

### AWS IAM Role Assumption

Always prefer roles over access keys:

```java
StsAssumeRoleCredentialsProvider.builder()
    .

refreshRequest(r ->r
    .

roleArn(roleArn)
        .

externalId(externalId)  // Prevents confused deputy
        .

durationSeconds(3600))
    .

build();
```

### Flyway Timestamp Triggers

All tables use `update_updated_at_column()` trigger:

```sql
CREATE TRIGGER update_my_table_updated_at
    BEFORE UPDATE
    ON my_table
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### Frontend Polling Pattern

```typescript
useEffect(() => {
  if (scanJob?.status === 'RUNNING') {
    const interval = setInterval(fetchStatus, 3000);
    return () => clearInterval(interval);  // CRITICAL
  }
}, [scanJob?.status]);
```

## Common Gotchas

### H2 vs PostgreSQL

```java
// ❌ Fails in H2 tests
Map.of("key",null)

// ✅ Works in both
Map<String, Object> map = new HashMap<>();
map.

put("key",null);
```

### AWS S3 No-Tags Handling

```java
// S3 throws 404 when bucket has no tags - this is normal
try{
    return s3.getBucketTagging(request).

tagSet();
}catch(
S3Exception e){
    if(e.

statusCode() ==404){
    return Collections.

emptyList();
    }
        throw e;
}
```

### AWS SDK Method Names

```java
// ❌ Wrong - has*() are boolean checks
bucket.hasTags()

// ✅ Correct - use getters
bucket.

serverSideEncryptionConfiguration()
```

### Database UUID Generation

```java

@Id
@GeneratedValue(strategy = GenerationType.UUID)  // NOT AUTO
private UUID id;
```

## Preferences

- Provide code implementations, not documentation
- No README/INSTALLATION/GUIDE files unless explicitly requested
- Brief technical summaries only
- Use arrow functions for React components
- Prefer `const` over `let`
- Use `type` instead of `interface` for simple types
- Always include error handling in API calls
- Controllers pass `user.getId()` to services, not the User entity
- DTOs use static factory methods: `MyResponse.fromEntity(entity)`
- Integration tests use `@Transactional` for auto-rollback
- Status enums uppercase with underscores: `OPEN`, `RUNNING`, `SUCCESS`