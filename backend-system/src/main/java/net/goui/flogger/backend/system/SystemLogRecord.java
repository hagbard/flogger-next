package net.goui.flogger.backend.system;

import com.google.common.flogger.LogContext;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.Metadata;
import com.google.common.flogger.backend.system.AbstractLogRecord;
import net.goui.flogger.backend.common.FloggerLogEntry;

/**
 * TODO: Maybe stop extending AbstractLogRecord to allow serialization?
 */
public final class SystemLogRecord extends AbstractLogRecord implements FloggerLogEntry {
  /** Creates a {@link SystemLogRecord} for a normal log statement from the given data. */
  static SystemLogRecord create(LogData data, Metadata scope) {
    return new SystemLogRecord(data, scope);
  }

  /** Creates a {@link SystemLogRecord} in the case of an error during logging. */
  static SystemLogRecord error(RuntimeException error, LogData data, Metadata scope) {
    return new SystemLogRecord(error, data, scope);
  }

  private SystemLogRecord(LogData data, Metadata scope) {
    super(data, scope);
    setThrown(getMetadataProcessor().getSingleValue(LogContext.Key.LOG_CAUSE));
    // See Flogger's SimpleLogRecord for why this is *essential* when using JDK log handlers.
    String unused = getMessage();
  }

  private SystemLogRecord(RuntimeException error, LogData data, Metadata scope) {
    // In the case of an error, the base class handles everything as there's no specific formatting.
    super(error, data, scope);
  }

  // Since the formatter is a function of our static options, it's effectively a singleton, so avoid
  // needing to add another field per log record by getting it from the factory.
  @Override
  public LogMessageFormatter getLogMessageFormatter() {
    return SystemBackendFactory.getFormatter();
  }
}
