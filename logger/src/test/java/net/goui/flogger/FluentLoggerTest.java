package net.goui.flogger;

import static com.google.common.flogger.LazyArgs.lazy;
import static com.google.common.truth.Truth.assertThat;
import static net.goui.flogger.testing.LevelClass.FINE;
import static net.goui.flogger.testing.LevelClass.INFO;
import static net.goui.flogger.testing.LevelClass.WARNING;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.goui.flogger.testing.LogEntry;
import net.goui.flogger.testing.junit4.FloggerTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FluentLoggerTest {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static {
    Logger.getLogger(FluentLoggerTest.class.getName()).setLevel(Level.INFO);
  }

  // NOTE: Set the forced level to higher than INFO, because we want to test rate limiting.
  @Rule
  public final FloggerTestRule logs = FloggerTestRule.forClass(FluentLoggerTest.class, INFO);

  @Test
  public void testEvaluation() {
    int x = 23;
    int y = 19;
    logger.atInfo(). "\{ x } + \{ y } = \{ lazy(() -> x + y) }" .log();
    LogEntry entry = logs.assertLogs().withLevel(INFO).getOnlyMatch();
    // Normally testing exact message contents makes tests brittle, but these are logger tests.
    assertThat(entry.message()).isEqualTo("23 + 19 = 42");
  }

  @Test
  public void testArguments_notEvaluatedWhenDisabled() {
    TestArg arg = new TestArg("Spanish Inquisition");
    logger.atFine(). "Unexected: \{ arg }" .log();
    logger.atFine(). "Unexected: \{ lazy(arg::toString) }" .log();
    logs.assertLogs().withLevel(FINE).doNotOccur();
    arg.assertCalled(0);
  }

  @Test
  public void testLazyArgs_evaluatedOnlyOnce() {
    TestArg arg = new TestArg("Hello World");
    logger.atInfo(). "Lazy Greeting: \{ lazy(arg::toString) }" .log();
    LogEntry entry = logs.assertLogs().withLevel(INFO).getOnlyMatch();
    assertThat(entry.message()).isEqualTo("Lazy Greeting: Hello World");
    arg.assertCalled(1);
  }

  private static final class TestArg {
    private final String message;
    private final AtomicInteger callCount = new AtomicInteger();

    private TestArg(String message) {
      this.message = message;
    }

    void assertCalled(int n) {
      assertThat(callCount.get()).isEqualTo(n);
    }

    @Override
    public String toString() {
      callCount.incrementAndGet();
      return message;
    }
  }
}
