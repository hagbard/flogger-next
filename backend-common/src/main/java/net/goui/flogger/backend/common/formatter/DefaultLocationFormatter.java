package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.LogSite;
import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.MetadataProcessor;
import net.goui.flogger.backend.common.MessageFormatter;
import net.goui.flogger.backend.common.Options;

class DefaultLocationFormatter implements MessageFormatter {
  DefaultLocationFormatter(Options options) {}

  @Override
  public void append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    LogSite logSite = logData.getLogSite();
    buffer.append(logSite.getClassName()).append("#").append(logSite.getMethodName());
  }
}
