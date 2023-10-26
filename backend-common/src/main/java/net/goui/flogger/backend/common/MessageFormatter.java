package net.goui.flogger.backend.common;

import com.google.common.flogger.MetadataKey;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import com.google.common.flogger.backend.SimpleMessageFormatter;
import java.util.List;
import java.util.Map;

/** Formatting API for the main body of a log message. */
public interface MessageFormatter {

  static LogMessageFormatter newFloggerFormatter(Options options) {
    return new FormatterAdapter(
        FloggerPlugin.instantiate(
            MessageFormatter.class,
            options,
            Map.of("default", MessageFormatter::getDefaultFormatter)));
  }

  /** Formats the log message and metadata into the given buffer. */
  void append(LogData logData, MetadataProcessor metadata, StringBuilder buffer);

  private static MessageFormatter getDefaultFormatter(Options options) {
    List<MetadataKey<?>> keysToIgnore = options.getValueArray("metadata.ignore", RenameThisClass::loadMetadataKey);
    if (keysToIgnore.isEmpty()) {
      return SimpleMessageFormatter.getDefaultFormatter()::append;
    } else {
      MetadataKey<?>[] keys = keysToIgnore.toArray(MetadataKey<?>[]::new);
      return SimpleMessageFormatter.getSimpleFormatterIgnoring(keys)::append;
    }
  }

  final class FormatterAdapter extends LogMessageFormatter {
    private final MessageFormatter formatter;

    FormatterAdapter(MessageFormatter formatter) {
      this.formatter = formatter;
    }

    @Override
    public StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
      formatter.append(logData, metadata, buffer);
      return buffer;
    }
  }
}
