package net.goui.flogger.backend.common;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.MetadataProcessor;

/**
 * Extends the core {@link FloggerLogEntry} API to enable more efficient message formatting.
 *
 * <p>This provides a common Flogger interface for passing structured data to backend modules,
 * implemented by "log record" classes from other logging frameworks (e.g. JDK logging or Log4J).
 *
 * <p>This interface is only valid when used in the same thread as the current log statement. It
 * must never be passed outside the immediate scope of a log statement call.
 */
public interface FloggerLogEntry {
  /** Returns the immutable Flogger {@link LogData} for the current log statement. */
  LogData getLogData();

  /**
   * Returns the immutable {@link MetadataProcessor} for the contextual and log-site metadata of the
   * current log statement (this may be created on demand, but must be an idempotent).
   */
  MetadataProcessor getMetadataProcessor();

  /**
   * Appends the Flogger log message, formatted according to the configured {@code
   * LogMessageFormatter}, to the given {@link StringBuilder}.
   */
  StringBuilder appendFormattedMessageTo(StringBuilder buffer);
}
