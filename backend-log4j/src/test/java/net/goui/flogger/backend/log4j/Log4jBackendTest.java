package net.goui.flogger.backend.log4j;

import static com.google.common.flogger.LogSites.logSite;
import static com.google.common.truth.Truth.assertThat;
import static net.goui.flogger.testing.truth.LogSubject.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.LogSite;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.testing.FakeLogData;
import java.util.logging.Level;
import net.goui.flogger.testing.LevelClass;
import net.goui.flogger.testing.LogEntry;
import net.goui.flogger.testing.SetLogLevel;
import net.goui.flogger.testing.junit4.FloggerTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Log4jBackendTest {
  @Rule
  public final FloggerTestRule logged =
      FloggerTestRule.forClass(Log4jBackendTest.class, LevelClass.INFO);

  // Should match the INFO entry in the XML config.
  private static final String BACKEND_NAME = "net.goui.flogger.backend";

  private static StringBuilder appendTestFormat(LogData data, StringBuilder out) {
    return out.append(data.getLevel()).append(" <<").append(data.getLiteralArgument()).append(">>");
  }

  private static final LogMessageFormatter TEST_FORMATTER =
      new LogMessageFormatter() {
        @Override
        public StringBuilder append(LogData data, MetadataProcessor metadata, StringBuilder out) {
          return appendTestFormat(data, out);
        }
      };

  @Test
  public void testBackendLog() {
    // This is all tested by the binding of the test rule anyway, since that also logs manually to a
    // backend instance to determine the support level.
    Log4jBackend backend = new Log4jBackend(BACKEND_NAME, TEST_FORMATTER);

    // Note that because the test rule is configured for the class under test, the log site must
    // match, even though the backend is for the package (there is additional filtering added by
    // the testing framework to ensure exact class matching).
    backend.log(FakeLogData.of("message 1").setLogSite(logSite()).setLevel(Level.INFO));

    // Messages too fine for the test rule are ignored.
    backend.log(FakeLogData.of("disabled log").setLogSite(logSite()).setLevel(Level.FINE));

    // Messages from other classes (to the same backend) are ignored.
    @SuppressWarnings("deprecation")
    LogSite otherClass =
        LogSite.injectedLogSite(BACKEND_NAME + ".OtherClass", "someMethod", 123, "");
    backend.log(FakeLogData.of("different class").setLogSite(otherClass).setLevel(Level.WARNING));

    backend.log(FakeLogData.of("message 2").setLogSite(logSite()).setLevel(Level.INFO));

    ImmutableList<LogEntry> logs = logged.assertLogs().getAllMatches();
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).message().isEqualTo("INFO <<message 1>>");
    assertThat(logs.get(1)).message().isEqualTo("INFO <<message 2>>");
  }

  // Add the Log4J backend to what's captured for this test as it is where the warning comes from.
  @Test
  @SetLogLevel(target = Log4jBackend.class, level = LevelClass.WARNING)
  public void handleError() {
    Log4jBackend backend = new Log4jBackend(BACKEND_NAME, TEST_FORMATTER);

    RuntimeException error = new IllegalStateException("Don't do that!");
    backend.handleError(error, FakeLogData.of("Oopsie").setLogSite(logSite()).setLevel(Level.INFO));

    logged.assertLogs().withLevel(LevelClass.INFO).doNotOccur();
    LogEntry errorLog = logged.assertLogs().withLevel(LevelClass.WARNING).getOnlyMatch();

    assertThat(errorLog).message().contains("LOGGING ERROR: Don't do that!");
    assertThat(errorLog).message().contains("original message: Oopsie");
    assertThat(errorLog).cause().isSameInstanceAs(error);
  }

  @Test
  public void getLoggerName() {
    // No name mapping happens for the underlying logger backends.
    assertThat(new Log4jBackend(BACKEND_NAME, TEST_FORMATTER).getLoggerName())
        .isEqualTo(BACKEND_NAME);
    assertThat(new Log4jBackend(BACKEND_NAME + ".OtherClass", TEST_FORMATTER).getLoggerName())
        .isEqualTo(BACKEND_NAME + ".OtherClass");
  }

  @Test
  public void isLoggable() {
    Log4jBackend infoBackend = new Log4jBackend(BACKEND_NAME, TEST_FORMATTER);
    // Log levels are set in the XML configuration for this module.
    assertThat(infoBackend.isLoggable(Level.WARNING)).isTrue();
    assertThat(infoBackend.isLoggable(Level.INFO)).isTrue();
    assertThat(infoBackend.isLoggable(Level.FINE)).isFalse();

    // A separate logger configured to level "warn" in the XML config.
    Log4jBackend warnBackend = new Log4jBackend(BACKEND_NAME + ".warning", TEST_FORMATTER);
    assertThat(warnBackend.isLoggable(Level.WARNING)).isTrue();
    assertThat(warnBackend.isLoggable(Level.INFO)).isFalse();
  }
}
