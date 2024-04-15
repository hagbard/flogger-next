package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.LogSite;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import net.goui.flogger.backend.common.Options;

final class DefaultLocationFormatter extends LogMessageFormatter {
  DefaultLocationFormatter(Options unused) {}

  @Override
  public StringBuilder append(
      LogData logData, MetadataProcessor metadataProcessor, StringBuilder buffer) {
    LogSite logSite = logData.getLogSite();
    buffer.append(logSite.getClassName()).append("#").append(logSite.getMethodName());
    return buffer;
  }
}
