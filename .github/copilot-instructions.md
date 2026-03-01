# GitHub Copilot Instructions

## Project overview

`tmwid-utils` is a focused Java utility library (`com.tmwid.utils`). Its current scope is console progress bar rendering. Keep additions small, purposeful, and consistent with the existing style.

## Language and build

- **Java 11** minimum — do not use language features beyond Java 11 unless explicitly asked.
- **Maven** is the only build tool; do not introduce Gradle or other build systems.
- All production sources live under `src/main/java/com/tmwid/utils/`.
- All test sources live under `src/test/java/com/tmwid/utils/`.

## Coding conventions

- Classes are `final` by default unless inheritance is genuinely needed.
- Prefer constructor injection over setters; keep fields `private final` where possible.
- Mutable shared state **must** be guarded by `synchronized` methods (the existing `ProgressBar` is the reference model — do not introduce `java.util.concurrent.locks` unless measurably necessary).
- Use `System.out` directly for console output — do not add a logging framework.
- No external runtime dependencies; test-scope JUnit Jupiter is the only allowed dependency.

## Naming

- Classes: `UpperCamelCase`.
- Methods and variables: `lowerCamelCase`.
- Constants: `UPPER_SNAKE_CASE` with `private static final`.
- Unit noun parameters follow the pattern used in `ProgressBar` (e.g. `"files"`, `"videos"`, `"items"`).

## Javadoc

- Every public class and every public/package-private method must have a Javadoc comment.
- Use `{@code}` for inline code, `{@link}` for cross-references, and `<pre>{@code …}</pre>` for multi-line examples.
- Document thread-safety guarantees explicitly on any class with shared mutable state.

## Testing

- Use **JUnit Jupiter 5** (`org.junit.jupiter.api.*`).
- Test class names: `<ClassUnderTest>Test` in the same package as the production class.
- One `@Test` method per logical behaviour; name tests as `methodName_descriptionOfBehaviour`.
- Capture `System.out` via `ByteArrayOutputStream` / `PrintStream` as shown in `ProgressBarTest`; do not use mocking frameworks.
- Aim for full branch coverage of rendering logic and edge cases (zero total, over-advance, concurrency).

## What to avoid

- Do not add GUI, Swing, or JavaFX code.
- Do not introduce ANSI escape sequences or colour unless explicitly requested (carriage-return overwrite is current strategy).
- Do not add reflection, annotation processing, or code-generation frameworks.
- Do not modify `pom.xml` packaging or group/artifact coordinates without explicit approval.
