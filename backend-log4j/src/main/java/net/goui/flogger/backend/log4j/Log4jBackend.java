package net.goui.flogger.backend.log4j;

import static net.goui.flogger.backend.log4j.Log4jEventUtil.getLog4jLevel;

import com.google.common.flogger.LogContext;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.MessageUtils;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.backend.Platform;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Objects;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

/** Flogger backend integration with Log4J2, using a lazily initialized Log4J logger. */
final class Log4jBackend extends LoggerBackend {
  private final String backendName;
  // Lazily initialized underlying Log4J logger instance. The LogManager is not required to return
  // the same instance on repeated calls for the same backend name, but they should be equivalent.
  @LazyInit @CheckForNull private Logger logger;
  private final LogMessageFormatter formatter;

  Log4jBackend(String backendName, LogMessageFormatter formatter) {
    this.backendName = Objects.requireNonNull(backendName);
    this.formatter = formatter;
  }

  private Logger lazyLogger() {
    // @LazyInit pattern: http://jeremymanson.blogspot.com/2008/12/benign-data-races-in-java.html
    Logger localRef = logger;
    if (localRef == null) {
      logger = localRef = (Logger) LogManager.getLogger(backendName);
    }
    return localRef;
  }

  @Override
  public void log(LogData data) {
    MetadataProcessor metadata =
        MetadataProcessor.forScopeAndLogSite(Platform.getInjectedMetadata(), data.getMetadata());

    Throwable thrown = metadata.getSingleValue(LogContext.Key.LOG_CAUSE);
    // Lazy log message which can append directly to an existing buffer.
    Log4jMessage log4jMessage = new Log4jMessage(b -> formatter.append(data, metadata, b), thrown);
    Thread currentThread = Thread.currentThread();
    Log4jLogEvent logEvent =
        Log4jLogEvent.newBuilder()
            .setLevel(Log4jEventUtil.getLog4jLevel(data.getLevel()))
            .setNanoTime(data.getTimestampNanos())
            .setLoggerName(data.getLoggerName())
            .setLoggerFqcn(data.getLogSite().getClassName())
            .setSource(Log4jEventUtil.getLog4jSource(data.getLogSite()))
            .setMessage(log4jMessage)
            // A ThrownProxy is created from this in the built event.
            .setThrown(thrown)
            .setContextData(Log4jEventUtil.createContextMap(metadata))
            // This is calculated on demand in the event, but we might be in a different thread.
            .setThreadName(currentThread.getName())
            .setThreadPriority(currentThread.getPriority())
            // Switch to currentThread.threadId() after JDK 19+ is standard.
            .setThreadId(currentThread.getId())
            .build();
    lazyLogger().get().log(logEvent);
  }

  @Override
  public void handleError(RuntimeException error, LogData badData) {
    lazyLogger().warn(formatLogErrorMessage(badData, error), error);
  }

  @Override
  public String getLoggerName() {
    return backendName;
  }

  @Override
  public boolean isLoggable(Level level) {
    return lazyLogger().isEnabled(getLog4jLevel(level));
  }

  private static String formatLogErrorMessage(LogData logData, RuntimeException error) {
    StringBuilder errorMsg =
        new StringBuilder("LOGGING ERROR: ").append(error.getMessage()).append('\n');
    int length = errorMsg.length();
    try {
      appendLogData(logData, errorMsg);
    } catch (RuntimeException e) {
      // Reset partially written buffer when an error occurs.
      errorMsg.setLength(length);
      errorMsg.append("Cannot append LogData: ").append(e);
    }
    return errorMsg.toString();
  }

  /** Appends the given {@link LogData} to the given {@link StringBuilder}. */
  private static void appendLogData(LogData data, StringBuilder out) {
    out.append("  original message: ");
    if (data.getTemplateContext() == null) {
      out.append(data.getLiteralArgument());
    } else {
      // We know that there's at least one argument to display here.
      out.append(data.getTemplateContext().getMessage());
      out.append("\n  original arguments:");
      for (Object arg : data.getArguments()) {
        out.append("\n    ").append(MessageUtils.safeToString(arg));
      }
    }
    Metadata metadata = data.getMetadata();
    if (metadata.size() > 0) {
      out.append("\n  metadata:");
      for (int n = 0; n < metadata.size(); n++) {
        out.append("\n    ");
        out.append(metadata.getKey(n).getLabel()).append(": ").append(metadata.getValue(n));
      }
    }
    out.append("\n  level: ").append(data.getLevel());
    out.append("\n  timestamp (nanos): ").append(data.getTimestampNanos());
    out.append("\n  class: ").append(data.getLogSite().getClassName());
    out.append("\n  method: ").append(data.getLogSite().getMethodName());
    out.append("\n  line number: ").append(data.getLogSite().getLineNumber());
  }
}
