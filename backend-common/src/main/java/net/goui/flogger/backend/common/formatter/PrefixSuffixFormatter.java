package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.MetadataProcessor;
import net.goui.flogger.backend.common.MessageFormatter;

class PrefixSuffixFormatter implements MessageFormatter {
  private final String prefix;
  private final String suffix;
  private final MessageFormatter delegate;

  PrefixSuffixFormatter(String prefix, String suffix, MessageFormatter delegate) {
    this.prefix = prefix;
    this.suffix = suffix;
    this.delegate = delegate;
  }

  @Override
  public void append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    int start = buffer.length();
    buffer.append(prefix);
    delegate.append(logData, metadata, buffer);
    if (buffer.length() == start + prefix.length()) {
      buffer.setLength(start);
    } else {
      buffer.append(suffix);
    }
  }
}
