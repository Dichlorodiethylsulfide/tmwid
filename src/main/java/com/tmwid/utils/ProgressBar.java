package com.tmwid.utils;

/**
 * A thread-safe console progress bar for tracking completion of a fixed-size work set.
 *
 * <p>{@link #advance()} and {@link #detail(String)} are safe to call concurrently from multiple
 * threads. All rendering is performed on {@link System#out} using a carriage-return overwrite
 * strategy so the bar occupies a single line.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * ProgressBar bar = new ProgressBar("CDN", "videos", 42, verbose);
 * bar.start();
 * for (ApiDocument doc : docs) {
 *     process(doc);
 *     bar.advance();
 * }
 * bar.finish("all OK");
 * }</pre>
 */
public final class ProgressBar {

  /** Width of the {@code #---} fill portion in characters. */
  private static final int BAR_WIDTH = 28;

  /** Label printed before the percentage, e.g. {@code "CDN"} or {@code "Render"}. */
  private final String label;

  /** Human-readable unit for the counter, e.g. {@code "videos"} or {@code "files"}. */
  private final String unit;

  /** Total number of work items (denominator). */
  private final int total;

  /**
   * When {@code true}, messages passed to {@link #detail(String)} are printed to stdout between
   * progress bar renders. When {@code false} they are silently suppressed.
   */
  private final boolean verbose;

  // Mutable state — all access must be via synchronized methods.
  private int completed;
  private int lastLineLength;
  private boolean started;
  private boolean progressVisible;

  /**
   * @param label short tag shown before the percentage, e.g. {@code "CDN"}
   * @param unit noun used in the {@code completed/total} counter, e.g. {@code "videos"}
   * @param total total number of work items; passing {@code 0} renders a full bar immediately
   * @param verbose when {@code true}, {@link #detail(String)} messages are printed
   */
  public ProgressBar(String label, String unit, int total, boolean verbose) {
    this.label = label;
    this.unit = unit;
    this.total = total;
    this.verbose = verbose;
  }

  /**
   * Convenience constructor using {@code "items"} as the unit.
   *
   * @param label short tag shown before the percentage
   * @param total total number of work items
   * @param verbose when {@code true}, detail messages are printed
   */
  public ProgressBar(String label, int total, boolean verbose) {
    this(label, "items", total, verbose);
  }

  /** Renders the initial 0 % bar and marks the bar as active. */
  public synchronized void start() {
    started = true;
    renderProgress(null, false);
  }

  /**
   * Increments the completed count by one and re-renders the bar. Safe to call from any thread.
   */
  public synchronized void advance() {
    if (completed < total) {
      completed++;
    }
    renderProgress(null, false);
  }

  /**
   * Completes the bar: renders a final line with {@code summary} appended, then moves to a new
   * line. Subsequent {@link #detail(String)} calls are passed through without bar manipulation.
   *
   * @param summary short result description appended after {@code "| "}, e.g. {@code "all OK"} or
   *     {@code "3 failure(s)"}; may be {@code null} to omit
   */
  public synchronized void finish(String summary) {
    renderProgress(summary, true);
    started = false;
  }

  /**
   * Prints a detail message to stdout, temporarily clearing the progress bar line if it is
   * currently visible, then re-rendering it afterward. When {@code verbose} is {@code false} the
   * message is silently suppressed.
   *
   * <p>Safe to call concurrently with {@link #advance()}.
   *
   * @param message the message to print
   */
  public synchronized void detail(String message) {
    if (!verbose) {
      return;
    }
    clearProgressLine();
    System.out.println(message);
    if (started) {
      renderProgress(null, false);
    }
  }

  // ---------------------------------------------------------------------------
  // Internal rendering
  // ---------------------------------------------------------------------------

  /**
   * Renders (or re-renders) the progress bar to stdout using {@code \r} overwrite.
   *
   * @param summary optional text appended after the counter; {@code null} omits it
   * @param newline when {@code true}, terminates with a newline rather than {@code \r}
   */
  private void renderProgress(String summary, boolean newline) {
    double ratio = total == 0 ? 1.0 : (double) completed / total;
    int percent = (int) Math.round(ratio * 100);
    int filled = total == 0 ? BAR_WIDTH : (int) Math.round(ratio * BAR_WIDTH);
    String bar = "#".repeat(filled) + "-".repeat(BAR_WIDTH - filled);
    String line =
        String.format(
            "  %s: %3d%%|%s| %d/%d %s%s",
            label,
            percent,
            bar,
            completed,
            total,
            unit,
            summary == null ? "" : " | " + summary);

    // Pad to overwrite any leftover characters from a longer previous line
    String padded =
        lastLineLength > line.length()
            ? line + " ".repeat(lastLineLength - line.length())
            : line;

    System.out.print("\r" + padded);
    System.out.flush();

    lastLineLength = line.length();
    progressVisible = !newline;

    if (newline) {
      System.out.println();
      System.out.flush();
      lastLineLength = 0;
    }
  }

  /** Blanks the current progress bar line so text can be printed above it cleanly. */
  private void clearProgressLine() {
    if (!progressVisible || lastLineLength == 0) {
      return;
    }
    System.out.print("\r" + " ".repeat(lastLineLength) + "\r");
    System.out.flush();
    progressVisible = false;
  }
}
