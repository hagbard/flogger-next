package net.goui.flogger.backend.log4j;

import static java.util.logging.Level.WARNING;

import com.google.common.flogger.LogContext.Key;
import com.google.common.flogger.LogSite;
import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.MetadataHandler;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.context.Tags;
import java.util.Set;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.util.StringMap;

/**
 * TODO: Docs
 */
final class Log4jEventUtil {
  /** Returns a {@link StackTraceElement} with the log site information in. */
  static StackTraceElement getLog4jSource(LogSite logSite) {
    return new StackTraceElement(
        logSite.getClassName(),
        logSite.getMethodName(),
        logSite.getFileName(),
        logSite.getLineNumber());
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
    if (key == Key.TAGS) {
      // Flatten tag values as entries directly in the context data.
      addTags(Key.TAGS.cast(value), kvh);
    } else if (value != null) {
      kvh.handle(key.getLabel(), value);
    }
  }

  private static void addTags(Tags tags, MetadataKey.KeyValueHandler kvh) {
    for (var e : tags.asMap().entrySet()) {
      String k = e.getKey();
      Set<Object> values = e.getValue();
      if (!values.isEmpty()) {
        for (Object v : values) {
          kvh.handle(k, v);
        }
      } else {
        // Tags without values are not allowed in ContextData, so fake it with an empty string.
        kvh.handle(k, "");
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
