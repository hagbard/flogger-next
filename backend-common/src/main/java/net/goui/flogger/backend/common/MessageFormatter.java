package net.goui.flogger.backend.common;

import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.backend.SimpleMessageFormatter;
import java.util.List;

/** Formatting API for the main body of a log message. */
public interface MessageFormatter {
  static LogMessageFormatter from(Options options) {
    String formatterName = options.getString("name", "default");
    if (Options.isAnyOf(formatterName, "default")) {
      return getDefaultFormatter(options);
    }
    return new FormatterAdapter(
        FloggerPlugin.instantiate(MessageFormatter.class, formatterName, options));
  }

  static List<MetadataKey<?>> loadIgnoredMetadataKeys(Options options) {
    return options.getValueArray("ignored_metadata", MessageFormatter::loadKey);
  }

  private static MetadataKey<?> loadKey(String keyName) {
    // Expected: "foo.bar.Class#Field"
    int idx = keyName.indexOf('#');
    if (idx == -1) {
      throw new IllegalStateException("Invalid metadata key name: " + keyName);
    }
    String className = keyName.substring(0, idx);
    String fieldName = keyName.substring(idx + 1);
    try {
      return (MetadataKey<?>) Class.forName(className).getDeclaredField(fieldName).get(null);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load metadata key: " + keyName, e);
    }
  }

  /**
   * Returns a formatted representation of the log message and metadata.
   *
   * <p>By default this method just returns:
   *
   * <pre>{@code append(logData, metadata, new StringBuilder()).toString()}</pre>
   *
   * <p>Formatter implementations may be able to implement it more efficiently (e.g. if they can
   * safely detect when no formatting is required).
   */
  default String format(LogData logData, MetadataProcessor metadata) {
    return append(logData, metadata, new StringBuilder()).toString();
  }

  /**
   * Formats the log message and metadata into the given buffer.
   *
   * @return the given buffer for method chaining.
   */
  StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer);

  static LogMessageFormatter getDefaultFormatter(Options options) {
    List<MetadataKey<?>> keysToIgnore = loadIgnoredMetadataKeys(options);
    if (keysToIgnore.isEmpty()) {
      return SimpleMessageFormatter.getDefaultFormatter();
    } else {
      MetadataKey<?>[] keys = keysToIgnore.toArray(MetadataKey<?>[]::new);
      return SimpleMessageFormatter.getSimpleFormatterIgnoring(keys);
    }
  }

  final class FormatterAdapter extends LogMessageFormatter {
    private final MessageFormatter formatter;

    FormatterAdapter(MessageFormatter formatter) {
      this.formatter = formatter;
    }

    @Override
    public String format(LogData logData, MetadataProcessor metadata) {
      return formatter.format(logData, metadata);
    }

    @Override
    public StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
      return formatter.append(logData, metadata, buffer);
    }
  }
}
