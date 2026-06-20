# Validation — Custom Annotations & Constants

## Custom composed annotations

### @ValidHandle

- Targets `ElementType.FIELD` and `ElementType.PARAMETER`.
- Handle: 4–15 chars, alphanumeric + underscore, cannot be purely numeric.
- Human-readable message attributes added directly to the `@Pattern` annotations inside —
  outer annotation's message attribute is unused because `@ReportAsSingleViolation` is not
  present; each composed constraint reports its own message independently.
- `@Size` messages remain as Jakarta defaults — accepted decision.

### @ValidPassword

- Password: 10+ chars, must contain uppercase, lowercase, number, and special character.
- Same composed annotation pattern.

## ValidationConstants

- Regex patterns extracted to `ValidationConstants` class.
- `MAX_MESSAGE_CONTENT_SIZE` used across `CreatePostInput`, `CreateReplyInput`,
  `CreateQuoteInput`.

## Design decision — duplicating validation annotations across input records

`CreatePostInput`, `CreateReplyInput`, and `CreateQuoteInput` share `messageContent`
validation annotations. Records cannot be extended. Constants are centralised in
`ValidationConstants` — rules defined once, only annotation declarations repeated. Acceptable
because inputs may diverge independently and overhead of a custom composed annotation isn't
justified for two annotations on one field.

## Key lessons

- Jakarta Bean Validation (Hibernate Validator) has no Spring dependency — constraints can be
  unit tested with `Validation.buildDefaultValidatorFactory().getValidator()` directly.
- `@Validated` on service class enables Bean Validation on method parameters.
- `@ValidHandle` AOP proxy not active in unit tests (no Spring context).
