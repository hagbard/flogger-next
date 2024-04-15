package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.logging.Level;
import net.goui.flogger.backend.common.Options;

final class DefaultLevelFormatter extends LogMessageFormatter {
  private final boolean useLocalizedName;

  DefaultLevelFormatter(Options options) {
    this.useLocalizedName = options.getBoolean("use_localized_name", false);
  }

  @Override
  public StringBuilder append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    Level level = logData.getLevel();
    buffer.append(useLocalizedName ? level.getLocalizedName() : level.getName());
    return buffer;
  }
}
