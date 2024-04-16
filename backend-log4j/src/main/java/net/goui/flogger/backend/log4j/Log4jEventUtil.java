package net.goui.flogger.backend.log4j;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.WARNING;

import com.google.common.flogger.LogContext.Key;
import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.MetadataHandler;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.context.Tags;
import java.util.Set;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.util.StringMap;

final class Log4jEventUtil {
  @SuppressWarnings({"NanosTo_Seconds", "SecondsTo_Nanos"})
  static Instant getLog4jInstantFromNanos(long timestampNanos) {
    MutableInstant instant = new MutableInstant();
    // Don't use Duration here as (a) it allocates and (b) we can't allow error on overflow.
    long epochSeconds = NANOSECONDS.toSeconds(timestampNanos);
    int remainingNanos = (int) (timestampNanos - SECONDS.toNanos(epochSeconds));
    instant.initFromEpochSecond(epochSeconds, remainingNanos);
    return instant;
  }

  /** Converts java.util.logging.Level to org.apache.log4j.Level. */
  static org.apache.logging.log4j.Level getLog4jLevel(java.util.logging.Level level) {
    int logLevel = level.intValue();
    if (logLevel < java.util.logging.Level.FINE.intValue()) {
      return org.apache.logging.log4j.Level.TRACE;
    } else if (logLevel < java.util.logging.Level.INFO.intValue()) {
      return org.apache.logging.log4j.Level.DEBUG;
    } else if (logLevel < WARNING.intValue()) {
      return org.apache.logging.log4j.Level.INFO;
    } else if (logLevel < java.util.logging.Level.SEVERE.intValue()) {
      return org.apache.logging.log4j.Level.WARN;
    }
    return org.apache.logging.log4j.Level.ERROR;
  }

  private static final MetadataHandler<MetadataKey.KeyValueHandler> HANDLER =
      MetadataHandler.builder(Log4jEventUtil::handleMetadata).build();

  private static void handleMetadata(
      MetadataKey<?> key, Object value, MetadataKey.KeyValueHandler kvh) {
    kvh.handle(key.getLabel(), key == Key.TAGS ? toValueQueue(Key.TAGS.cast(value)) : value);
  }

  private static ValueQueue toValueQueue(Tags tags) {
    ValueQueue queue = new ValueQueue();
    // Flatten tags to treat them as keys or key/value pairs, e.g. ["baz=bar1", "baz=bar2", "foo"]
    tags.asMap().forEach((label, tagValues) -> addTagValues(queue, label, tagValues));
    return queue;
  }

  private static void addTagValues(ValueQueue queue, String tagLabel, Set<Object> tagValues) {
    if (tagValues.isEmpty()) {
      ValueQueue.concat(queue, tagLabel);
    } else {
      for (Object tagValue : tagValues) {
        ValueQueue.concat(queue, tagLabel + "=" + tagValue);
      }
    }
  }

  /**
   * We do not support {@code MDC.getContext()} and {@code NDC.getStack()} and we do not make any
   * attempt to merge Log4j2 context data with Flogger's context data. Instead, users should use the
   * {@code ScopedLoggingContext}.
   *
   * <p>Flogger's {@code ScopedLoggingContext} allows to include additional metadata and tags into
   * logs which are written from current thread. This context data will be added to the log4j2
   * event.
   */
  static StringMap createContextMap(MetadataProcessor metadataProcessor) {
    StringMap contextData = ContextDataFactory.createContextData(metadataProcessor.keyCount());
    metadataProcessor.process(
        HANDLER,
        (key, value) ->
            contextData.putValue(key, ValueQueue.concat(contextData.getValue(key), value)));
    contextData.freeze();
    return contextData;
  }

  private Log4jEventUtil() {}
}