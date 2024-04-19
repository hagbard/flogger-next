package net.goui.flogger.backend.system;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.list;
import static net.goui.flogger.testing.LevelClass.INFO;
import static net.goui.flogger.testing.LevelClass.WARNING;
import static net.goui.flogger.testing.truth.LogSubject.assertThat;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import net.goui.flogger.testing.LevelClass;
import net.goui.flogger.testing.LogEntry;
import net.goui.flogger.testing.junit4.FloggerTestRule;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SystemBackendFactoryTest {
  // We test in the package, because that's where the logging *comes from*, but the
  // UnknownLoggerClass has its backend name mapped to the *parent* of this package.
  // This demonstrates that (at least in most cases) logger name mapping DOES NOT
  // impact whether logs are captured or not.
  @Rule public final FloggerTestRule logged = FloggerTestRule.forPackageUnderTest(LevelClass.INFO);

  private static final String UNKNOWN_LOGGER_CLASS_BACKEND_NAME = "net.goui.flogger.backend";

  public static final class Key {
    public static final MetadataKey<String> FOO = MetadataKey.single("foo_key", String.class);
    public static final MetadataKey<String> BAR = MetadataKey.single("bar_key", String.class);
  }

  @BeforeClass
  public static void setTestProperties() {
    System.setProperty("java.util.logging.config.class", FloggerConfig.class.getName());
    System.setProperty(
        "java.util.logging.config.file",
        ClassLoader.getSystemResource("logging.properties").getPath());
  }

  @Test
  public void testLogging() {
    FluentLogger logger = FluentLogger.forEnclosingClass();
    logger
        .atInfo()
        .every(100)
        .with(Key.FOO, "<handled>")
        .with(Key.BAR, "<default>")
        .log("Hello World");
    logged
        .assertLogs()
        .withLevel(INFO)
        .withMessageContaining("Hello World")
        .matchCount()
        .isEqualTo(1);
  }

  @Test
  public void testLogging_argumentError() {
    FluentLogger logger = FluentLogger.forEnclosingClass();
    Object unexpected = new Object() {
      @Override
      public String toString() {
        throw new RuntimeException("Spanish Inquisition");
      }
    };

    logger.atInfo().log("Nobody expects: %s", unexpected);
    LogEntry errLog = logged
        .assertLogs()
        .withLevel(INFO)
        .withMessageContaining("Nobody expects:")
        .getOnlyMatch();
    assertThat(errLog).message().contains("java.lang.RuntimeException: Spanish Inquisition");
  }

  @Test
  public void testLogging_internalError() {
    FluentLogger logger = FluentLogger.forEnclosingClass();
    MetadataKey<String> badKey = new MetadataKey<>("label", String.class, false) {
      @Override
      protected void emit(String value, KeyValueHandler kvh) {
        throw new RuntimeException("Internal Error");
      }
    };

    logger.atInfo().with(badKey, "<unused>").log("Message");
    LogEntry errLog = logged
        .assertLogs()
        .withLevel(WARNING)
        .withMessageContaining("LOGGING ERROR")
        .getOnlyMatch();
    assertThat(errLog).message().contains("Internal Error");
    assertThat(errLog).message().contains("original message: Message");
  }

  @Test
  public void testJdkConfiguration_rootNames() {
    // The logging properties file defines 3 mappings, one for each of the test logger classes.
    // 1. SystemRootLogger: Configured to WARNING level and inherited as a Flogger root.
    // 2. FloggerRootLogger: Set as a logger root, but not configured.
    // 3. UnknownLoggerClass: Not a root entry, so shares a backend for the package.

    // All logs should appear since logging is forced at INFO in the test.
    SystemRootLogger.log(Level.INFO, "<System>");
    FloggerRootLogger.log(Level.INFO, "<Flogger>");
    UnknownLoggerClass.log(Level.INFO, "<Package>");

    // Check initial expectations for the underlying JDK loggers.
    ArrayList<String> loggerNames = list(LogManager.getLogManager().getLoggerNames());
    assertThat(loggerNames)
        .containsAtLeast(
            FloggerRootLogger.class.getName(),
            SystemRootLogger.class.getName(),
            UNKNOWN_LOGGER_CLASS_BACKEND_NAME);
    // Since UnknownLoggerClass has no root entry, it associates to the parent package, so no
    // JDK logger for the class itself is created.
    assertThat(loggerNames).doesNotContain(UnknownLoggerClass.class.getName());

    logged.assertLogs().withMessageContaining("<System>").matchCount().isEqualTo(1);
    logged.assertLogs().withMessageContaining("<Flogger>").matchCount().isEqualTo(1);
    logged.assertLogs().withMessageContaining("<Package>").matchCount().isEqualTo(1);
  }

  @Test
  public void testJdkConfiguration_levels() {
    // The logging properties file defines 3 mappings, one for each of the test logger classes.
    // 1. SystemRootLogger: Configured to WARNING level and inherited as a Flogger root.
    // 2. FloggerRootLogger: Set as a logger root, but not configured.
    // 3. UnknownLoggerClass: Not a root entry, so shares a backend for the package.

    // Test that each JDK logger backend has a 1-to-1 association with its expected logging class.
    Logger systemJdkLogger = Logger.getLogger(SystemRootLogger.class.getName());
    Logger floggerJdkLogger = Logger.getLogger(FloggerRootLogger.class.getName());
    Logger unknownJdkLogger = Logger.getLogger(UNKNOWN_LOGGER_CLASS_BACKEND_NAME);

    // Set by the logging properties loaded by this test (only check this after doing logging).
    assertThat(systemJdkLogger.getLevel()).isEqualTo(Level.WARNING);
    assertThat(floggerJdkLogger.getLevel()).isNull();
    assertThat(unknownJdkLogger.getLevel()).isNull();

    // Nothing enabled yet, but reconfigure each JDK logger in turn.
    assertThat(SystemRootLogger.isEnabled(Level.FINE)).isFalse();
    assertThat(FloggerRootLogger.isEnabled(Level.FINE)).isFalse();
    assertThat(UnknownLoggerClass.isEnabled(Level.FINE)).isFalse();

    systemJdkLogger.setLevel(Level.FINE);

    assertThat(SystemRootLogger.isEnabled(Level.FINE)).isTrue();
    assertThat(FloggerRootLogger.isEnabled(Level.FINE)).isFalse();
    assertThat(UnknownLoggerClass.isEnabled(Level.FINE)).isFalse();

    floggerJdkLogger.setLevel(Level.FINE);

    assertThat(SystemRootLogger.isEnabled(Level.FINE)).isTrue();
    assertThat(FloggerRootLogger.isEnabled(Level.FINE)).isTrue();
    assertThat(UnknownLoggerClass.isEnabled(Level.FINE)).isFalse();

    unknownJdkLogger.setLevel(Level.FINE);

    assertThat(SystemRootLogger.isEnabled(Level.FINE)).isTrue();
    assertThat(FloggerRootLogger.isEnabled(Level.FINE)).isTrue();
    assertThat(UnknownLoggerClass.isEnabled(Level.FINE)).isTrue();
  }
}
