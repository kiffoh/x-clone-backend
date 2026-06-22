# Auth — JWT + Refresh Token REST Authentication

Phases 1–3 complete. Full JWT + refresh token authentication system at `/api/auth`.
Security infrastructure (filter, config, token provider) lives in `../security/`.

## Endpoints

- `POST /signup` — creates user, returns access token, sets httpOnly refresh cookie
- `POST /login` — validates credentials, returns access token, sets httpOnly refresh cookie
- `POST /logout` — invalidates refresh token in Redis, clears cookie (requires Bearer token)
- `POST /refresh` — rotates refresh token, returns new access token (requires cookie only)

## Token strategy

- **Access token:** JWT, HMAC-SHA256, 15 minute lifetime, stored in-memory by client.
- **Refresh token:** UUID, 30 day lifetime, stored in Redis with TTL, delivered via
  httpOnly/Secure/SameSite=Strict cookie.

## Security model

- `JwtAuthenticationFilter` (`../security/jwt/`) validates Bearer tokens on protected routes.
- `GlobalExceptionHandler` returns consistent error shapes, never leaks internal messages.
- `BadCredentialsException` and `UsernameNotFoundException` merged into one 401 handler to
  prevent username enumeration.
- `CustomUserDetails` has `getId()` returning `UUID` directly; UUID conversion happens once in
  `JwtAuthenticationFilter`.

## Validation

- Custom composed annotations `@ValidHandle` and `@ValidPassword` using Jakarta constraints.
- `@ValidHandle` targets both `ElementType.FIELD` and `ElementType.PARAMETER` — used on DTO
  fields and service method parameters.
- Handle: 4–15 chars, alphanumeric + underscore, cannot be purely numeric.
- Password: 10+ chars, must contain uppercase, lowercase, number, and special character.
- Constants for regex patterns extracted to `ValidationConstants` class
  (`../validation/`).
- Human-readable message attributes added directly to the `@Pattern` annotations inside
  `@ValidHandle` and `@ValidPassword` — the outer annotation's message attribute is unused
  because `@ReportAsSingleViolation` is not present; each composed constraint reports its own
  message independently.
- `@Size` messages remain as Jakarta defaults ("size must be between X and Y") — accepted
  decision.

## OpenAPI / Swagger Documentation (Phase 3)

Production-quality REST API documentation for the auth layer:
- Single `@SecurityScheme` of type HTTP/bearer/JWT.
- Shared reusable response components.
- Per-endpoint `@ApiResponse` declarations with accurate status codes.
- Full constraint documentation on all DTO fields via `@Schema`.
- `@Tag` annotation for clean grouping under "Authentication".
- `ErrorResponse` and `ValidationErrorResponse` registered as named schema components via
  `ModelConverters`.

## Testing — complete

**Four testing phases:**
1. Exception handler unit tests.
2. Service unit tests.
3. Controller slice tests (`@WebMvcTest`).
4. Integration tests (`@SpringBootTest` + Testcontainers) covering full auth flow including
   Redis rotation.

**AuthenticationIT specifics:**
- Extends `BaseAuthIntegrationTest` (uses `TestRestTemplate`, not `HttpGraphQlTester`).
- Signup helper password: `"Password123!"` (must satisfy `@ValidPassword`).
- Login invalid credentials test password: `"passwordDoesNotMatch1!"` (must pass validation to
  reach credential checking — without valid format, returns 400 instead of 401).
