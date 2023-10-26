package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.logging.Level;
import net.goui.flogger.backend.common.MessageFormatter;
import net.goui.flogger.backend.common.Options;

class DefaultLevelFormatter implements MessageFormatter {
  private final boolean useLocalizedName;

  DefaultLevelFormatter(Options options) {
    this.useLocalizedName = options.getBoolean("use_localized_name", false);
  }

  @Override
  public void append(LogData logData, MetadataProcessor metadata, StringBuilder buffer) {
    Level level = logData.getLevel();
    buffer.append(useLocalizedName ? level.getLocalizedName() : level.getName());
  }
}
