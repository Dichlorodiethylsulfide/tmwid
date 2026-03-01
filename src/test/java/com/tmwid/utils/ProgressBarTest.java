package com.tmwid.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgressBarTest {

  private static final int BAR_WIDTH = 28;

  private ByteArrayOutputStream captured;
  private PrintStream originalOut;

  @BeforeEach
  void redirectStdout() {
    originalOut = System.out;
    captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured));
  }

  @AfterEach
  void restoreStdout() {
    System.setOut(originalOut);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /** Returns all stdout written so far with carriage returns stripped for easy comparison. */
  private String output() {
    return captured.toString().replace("\r", "");
  }

  /** Returns the last non-blank "frame" written (last segment separated by \r). */
  private String lastFrame() {
    String raw = captured.toString();
    String[] parts = raw.split("\r");
    for (int i = parts.length - 1; i >= 0; i--) {
      String s = parts[i].stripTrailing();
      if (!s.isBlank()) return s;
    }
    return raw;
  }

  private static String fullBar() {
    return "#".repeat(BAR_WIDTH);
  }

  private static String emptyBar() {
    return "-".repeat(BAR_WIDTH);
  }

  // ---------------------------------------------------------------------------
  // start()
  // ---------------------------------------------------------------------------

  @Test
  void start_rendersZeroPercentBar() {
    ProgressBar bar = new ProgressBar("Work", "files", 5, false);
    bar.start();

    String out = output();
    assertTrue(out.contains("  Work:   0%"), "should show 0%: " + out);
    assertTrue(out.contains("0/5 files"), "should show 0/5 files: " + out);
    assertTrue(out.contains(emptyBar()), "should show empty bar: " + out);
  }

  @Test
  void start_usesLabelAndUnit() {
    ProgressBar bar = new ProgressBar("Render", "frames", 10, false);
    bar.start();

    String out = output();
    assertTrue(out.contains("  Render:"), "label should appear: " + out);
    assertTrue(out.contains("frames"), "unit should appear: " + out);
  }

  // ---------------------------------------------------------------------------
  // advance()
  // ---------------------------------------------------------------------------

  @Test
  void advance_incrementsCounterOnEachCall() {
    ProgressBar bar = new ProgressBar("T", "items", 4, false);
    bar.start();

    bar.advance();
    assertTrue(output().contains("1/4 items"), "after 1 advance: " + output());

    bar.advance();
    assertTrue(output().contains("2/4 items"), "after 2 advances: " + output());
  }

  @Test
  void advance_updatesPercentCorrectly() {
    ProgressBar bar = new ProgressBar("T", "x", 4, false);
    bar.start();
    bar.advance(); // 25 %
    bar.advance(); // 50 %

    // The last rendered frame should show 50 %
    assertTrue(lastFrame().contains(" 50%"), "should show 50%: " + lastFrame());
  }

  @Test
  void advance_doesNotExceedTotal() {
    ProgressBar bar = new ProgressBar("T", "x", 3, false);
    bar.start();

    // Advance far beyond total
    for (int i = 0; i < 10; i++) {
      bar.advance();
    }

    // The output should never contain "4/3" or higher completed counts
    String out = output();
    assertFalse(out.contains("4/3"), "counter must not exceed total: " + out);
    assertTrue(out.contains("3/3"), "should reach 3/3: " + out);
  }

  @Test
  void advance_fullBarWhenAllItemsComplete() {
    ProgressBar bar = new ProgressBar("T", "x", 3, false);
    bar.start();
    bar.advance();
    bar.advance();
    bar.advance();

    assertTrue(lastFrame().contains(fullBar()), "bar should be full: " + lastFrame());
    assertTrue(lastFrame().contains("100%"), "should show 100%: " + lastFrame());
  }

  // ---------------------------------------------------------------------------
  // finish()
  // ---------------------------------------------------------------------------

  @Test
  void finish_appendsSummaryAfterPipe() {
    ProgressBar bar = new ProgressBar("T", "x", 2, false);
    bar.start();
    bar.advance();
    bar.advance();
    bar.finish("all OK");

    assertTrue(output().contains("| all OK"), "summary should appear: " + output());
  }

  @Test
  void finish_withNullSummary_omitsPipeAndSummary() {
    ProgressBar bar = new ProgressBar("T", "x", 1, false);
    bar.start();
    bar.advance();
    bar.finish(null);

    // No summary segment — the line should end with the unit, not "| ..."  (strip trailing spaces)
    String out = output();
    // The finish line written just before the system newline must not contain " | "
    String[] frames = out.split("\n");
    String lastLine = frames[frames.length - 1].stripTrailing();
    assertFalse(lastLine.contains(" | "), "null summary should produce no ' | ': " + lastLine);
  }

  @Test
  void finish_terminatesWithNewline() {
    ProgressBar bar = new ProgressBar("T", "x", 1, false);
    bar.start();
    bar.finish("done");

    // After finish a real newline (not just \r) must be present in the raw output
    assertTrue(captured.toString().contains("\n"), "finish must emit a newline");
  }

  // ---------------------------------------------------------------------------
  // Zero-total
  // ---------------------------------------------------------------------------

  @Test
  void zeroTotal_rendersFullBarImmediately() {
    ProgressBar bar = new ProgressBar("T", "x", 0, false);
    bar.start();

    String out = output();
    assertTrue(out.contains("100%"), "zero-total bar should show 100%: " + out);
    assertTrue(out.contains(fullBar()), "zero-total bar should be full: " + out);
  }

  @Test
  void zeroTotal_advanceIsHarmless() {
    ProgressBar bar = new ProgressBar("T", "x", 0, false);
    bar.start();
    assertDoesNotThrow(() -> {
      bar.advance();
      bar.advance();
    });
  }

  // ---------------------------------------------------------------------------
  // detail()
  // ---------------------------------------------------------------------------

  @Test
  void detail_whenVerboseFalse_suppressesMessage() {
    ProgressBar bar = new ProgressBar("T", "x", 5, false);
    bar.start();
    bar.detail("secret message");

    assertFalse(output().contains("secret message"),
        "verbose=false should suppress detail: " + output());
  }

  @Test
  void detail_whenVerboseTrue_printsMessage() {
    ProgressBar bar = new ProgressBar("T", "x", 5, true);
    bar.start();
    bar.detail("hello world");

    assertTrue(output().contains("hello world"),
        "verbose=true should print detail: " + output());
  }

  @Test
  void detail_whenVerboseTrue_redrawsBarAfterMessage() {
    ProgressBar bar = new ProgressBar("T", "files", 5, true);
    bar.start();
    bar.advance(); // 1/5
    bar.detail("a message");

    // The bar must appear again after the detail message in the output stream
    String out = output();
    int msgIdx = out.indexOf("a message");
    int barAfterMsg = out.indexOf("1/5 files", msgIdx);
    assertTrue(barAfterMsg > msgIdx,
        "bar should be redrawn after detail message: " + out);
  }

  @Test
  void detail_afterFinish_neverRedrawsBar() {
    ProgressBar bar = new ProgressBar("T", "x", 1, true);
    bar.start();
    bar.advance();
    bar.finish("done");

    captured.reset(); // clear everything before finish
    bar.detail("post-finish message");

    // message should still be printed (verbose=true)
    assertTrue(output().contains("post-finish message"), output());
    // but no fresh bar line should follow (no "%|" pattern after the message)
    String out = output();
    int msgEnd = out.indexOf("post-finish message") + "post-finish message".length();
    assertFalse(out.substring(msgEnd).contains("%|"),
        "no bar redraw expected after finish: " + out);
  }

  // ---------------------------------------------------------------------------
  // Convenience constructor
  // ---------------------------------------------------------------------------

  @Test
  void convenienceConstructor_usesItemsAsUnit() {
    ProgressBar bar = new ProgressBar("Job", 3, false);
    bar.start();

    assertTrue(output().contains("items"), "default unit should be 'items': " + output());
  }

  // ---------------------------------------------------------------------------
  // Thread safety
  // ---------------------------------------------------------------------------

  @Test
  void concurrentAdvance_doesNotExceedTotal() throws InterruptedException {
    final int total = 50;
    final int threads = 100;

    ProgressBar bar = new ProgressBar("T", "x", total, false);
    bar.start();

    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(threads);

    for (int i = 0; i < threads; i++) {
      pool.submit(() -> {
        ready.countDown();
        try {
          go.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        bar.advance();
      });
    }

    ready.await();
    go.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

    String out = output();
    // completed must never exceed total — pattern "NN/<total>" where NN > total must not appear
    assertFalse(out.contains((total + 1) + "/" + total),
        "completed should never exceed total: last frame=" + lastFrame());
    assertTrue(lastFrame().contains(total + "/" + total),
        "should reach " + total + "/" + total + ": " + lastFrame());
  }

  @Test
  void concurrentAdvanceAndDetail_noExceptions() throws InterruptedException {
    final int total = 20;
    ProgressBar bar = new ProgressBar("T", "x", total, true);
    bar.start();

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < total; i++) {
      final int idx = i;
      threads.add(new Thread(() -> {
        bar.advance();
        bar.detail("detail from thread " + idx);
      }));
    }

    threads.forEach(Thread::start);
    for (Thread t : threads) t.join(5_000);

    // No assertion on output content — just verify no exception was thrown
    // and the bar reached total
    assertTrue(lastFrame().contains(total + "/" + total),
        "should eventually reach " + total + "/" + total + ": " + lastFrame());
  }
}
