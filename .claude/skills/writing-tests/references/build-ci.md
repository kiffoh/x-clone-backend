# Build & CI Configuration

## Maven Failsafe Plugin

- Added to `pom.xml` — **required** for `*IT` test classes to execute during `mvnw verify`.
- Previously all integration tests were silently skipped in CI — Maven Surefire (included by
  default) only picks up `*Test` classes; `*IT` classes require Failsafe.
- Failsafe hooks into the `integration-test` and `verify` Maven lifecycle phases.
- This was undetected because tests were run individually from IntelliJ, which uses JUnit
  directly and bypasses Maven lifecycle phases.

## JaCoCo — Updated for Integration Test Coverage

- `prepare-agent-integration` execution added — instruments Failsafe test runs.
- `destFile` set to `${project.build.directory}/jacoco.exec` with `append: true` — merges IT
  coverage into the same file as unit test coverage so a single report includes both.
- `report` and `check` executions moved from `test` phase to `verify` phase — ensures the
  report is generated after both Surefire and Failsafe complete.

## SpotBugs

- Updated from `4.8.3.0` to `4.9.8.0` — the old version did not support class file major
  version 67 (Java 23).
- Error was `Unsupported class file major version 67` on core JDK classes (`java.lang.Object`,
  `java.lang.Record`, etc.).

## SpotBugs fixes applied

- `Cursor.encode()` — `String.getBytes()` replaced with
  `String.getBytes(StandardCharsets.UTF_8)` (`DM_DEFAULT_ENCODING`).
- `FollowService.followUser()` and `LikeService.createLike()` — null-safety guards on
  `getRootCause()` → `getServerErrorMessage()` → `getConstraint()` chain
  (`NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`).
- `GraphQlExceptionHandler.handleBindException()` — `ex.getFieldError()` result stored in
  local `org.springframework.validation.FieldError` variable before null-checking; FQN used
  to avoid collision with the project's `FieldError` type
  (`NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`).

## AuthenticationIT password fixes

- Signup helper password updated from `"password"` to `"Password123!"` — satisfies
  `@ValidPassword` constraints (10+ chars, uppercase, lowercase, number, special character).
- Login invalid credentials test password updated from `"passwordDoesNotMatch"` to
  `"passwordDoesNotMatch1!"` — must pass `@ValidPassword` validation to reach credential
  checking; without a valid password format, the request returns 400 instead of 401.
