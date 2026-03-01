# Tell Me When It's Done — Utils

`tmwid-utils` is a small Java utility library that provides a thread-safe, single-line console progress bar for tracking the completion of fixed-size work sets.

## Features

- Single-line, carriage-return–based rendering — the bar stays on one terminal line
- Thread-safe: `advance()` and `detail()` are safe to call from multiple threads concurrently
- Optional verbose mode prints per-item detail messages above the bar without corrupting its layout
- Configurable label, unit, and total count
- Padding logic prevents leftover characters when a detail message is shorter than the bar line

## Requirements

- Java 11+
- Maven 3.6+

## Build

```bash
mvn package
```

Run tests only:

```bash
mvn test
```

## Usage

Add the JAR (or install the artifact locally with `mvn install`) and import `ProgressBar`:

```java
import com.tmwid.utils.ProgressBar;

// Full constructor: label, unit, total, verbose
ProgressBar bar = new ProgressBar("CDN", "videos", 42, true);
bar.start();

for (ApiDocument doc : docs) {
    process(doc);
    bar.advance();           // thread-safe increment + re-render
    bar.detail(doc.name());  // printed only when verbose = true
}

bar.finish("all OK");        // finalises the bar and moves to a new line
```

Convenience constructor (unit defaults to `"items"`):

```java
ProgressBar bar = new ProgressBar("Render", 100, false);
```

### Output format

```
  CDN:  73%|####################--------| 31/42 videos
```

| Segment | Example | Description |
|---------|---------|-------------|
| Label   | `CDN:`  | Short tag passed to the constructor |
| Percent | `73%`   | Rounded percentage |
| Bar     | `####################--------` | 28-character fill (` #` filled, `-` empty) |
| Counter | `31/42 videos` | Completed / total + unit |
| Summary | `\| all OK` | Appended by `finish(summary)` only |

## Project layout

```
src/
  main/java/com/tmwid/utils/
    ProgressBar.java        # Production source
  test/java/com/tmwid/utils/
    ProgressBarTest.java    # JUnit Jupiter test suite
pom.xml
```

## Coordinates

| Property | Value |
|----------|-------|
| `groupId` | `com.tmwid` |
| `artifactId` | `tmwid-utils` |
| `version` | `1.0.0-SNAPSHOT` |

## License

See [LICENSE](LICENSE).
