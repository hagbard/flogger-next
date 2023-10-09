package net.goui.flogger.next;

import com.google.common.flogger.AbstractLogger;
import com.google.common.flogger.GoogleLogContext;
import com.google.common.flogger.GoogleLoggingApi;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.Platform;
import java.util.logging.Level;

/**
 * A drop in replacement for {@code com.google.common.flogger.FluentLogger} which supports both:
 *
 * <ul>
 *   <li>Logging via the proposed JDK 22 {@link StringTemplate} API.
 *   <li>Google specific APIs (if present) matching those in {@code GoogleLogger}.
 * </ul>
 *
 * <p>This lets you write log statements using the syntax:
 *
 * <pre>{@code
 * logger.atInfo()."eval(\{x}, \{y}) = \{lazy(() -> eval(x, y))}".log();
 * }</pre>
 *
 * <p>Use this class only if you are willing to use the (as of Oct 2023) preview {@link
 * StringTemplate} API (<a href="https://openjdk.org/jeps/430">JEP 430</a>), which risks changing at
 * short notice until it is accepted fully into the JDK.
 */
public final class FluentLogger extends AbstractLogger<NextLoggingApi> {
  private static final NoOp NO_OP = new NoOp();

  // Visible for testing only.
  FluentLogger(LoggerBackend backend) {
    super(backend);
  }

  /**
   * Returns a new logger instance which parses log messages using printf format for the enclosing
   * class using the system default logging backend.
   */
  public static FluentLogger forEnclosingClass() {
    // NOTE: It is _vital_ that the call to "caller finder" is made directly inside the static
    // factory method. See getCallerFinder() for more information.
    String loggingClass = Platform.getCallerFinder().findLoggingClass(FluentLogger.class);
    return new FluentLogger(Platform.getBackend(loggingClass));
  }

  @Override
  public NextLoggingApi at(Level level) {
    // Standard setup for creating a log context (copied com.google.common.flogger.FluentLogger).
    boolean isLoggable = isLoggable(level);
    boolean isForced = Platform.shouldForceLogging(getName(), level, isLoggable);
    return (isLoggable | isForced) ? new Context(level, isForced) : NO_OP;
  }

  // Extended log context (extending from GoogleLogContext for any extra APIs).
  private class Context extends GoogleLogContext<FluentLogger, NextLoggingApi>
      implements NextLoggingApi {
    protected Context(Level level, boolean isForced) {
      super(level, isForced);
    }

    @Override
    protected NextLoggingApi api() {
      return this;
    }

    @Override
    protected FluentLogger getLogger() {
      return FluentLogger.this;
    }

    @Override
    protected NextLoggingApi noOp() {
      return NO_OP;
    }

    @Override
    public LogString process(StringTemplate template) {
      // Until Flogger has a better notion of passing "pre-processed" log messages to the backend,
      // this will have to do.
      return () -> log("%s", LogTemplate.lazilyInterpolate(template));
    }
  }

  private static class NoOp extends GoogleLoggingApi.NoOp<NextLoggingApi>
      implements NextLoggingApi {
    @Override
    public LogString process(StringTemplate stringTemplate) throws RuntimeException {
      return () -> {};
    }
  }
}
