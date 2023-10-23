package net.goui.flogger.backend.common;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.MetadataProcessor;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Common Flogger interface for passing structured Flogger data to backend modules, implemented by
 * subclasses of "log records" from other logging frameworks (e.g. JDK logging or Log4J).
 *
 * <p>This interface is only valid when used in the same thread as the current log statement. It
 * must never be passed outside the immediate scope of a log statement call.
 */
public interface FloggerLogEntry {
  @NullableDecl
  static FloggerLogEntry getOrNull(Object anyRecord) {
    return anyRecord instanceof FloggerLogEntry ? (FloggerLogEntry) anyRecord : null;
  }

  /** Returns the immutable Flogger {@link LogData} for the current log statement. */
  LogData getLogData();

  /**
   * Returns the immutable {@link MetadataProcessor} for the contextual and log-site metadata of the
   * current log statement (this may be created on demand, but must be an idempotent).
   */
  MetadataProcessor getMetadataProcessor();
}
