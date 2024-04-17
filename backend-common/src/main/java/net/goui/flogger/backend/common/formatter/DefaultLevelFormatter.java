package net.goui.flogger.backend.common.formatter;

import com.google.common.flogger.backend.LogData;
import com.google.common.flogger.backend.LogMessageFormatter;
import com.google.common.flogger.backend.MetadataProcessor;
import java.util.logging.Level;
import net.goui.flogger.backend.common.Options;

/**
 * Flogger message formatter plugin for the {@code %{level}} directive to format the log level.
 *
 * <h3>Options</h3>
 *
 * <ul>
 *   <li>{@code message_formatter.level.use_localized_name}: Boolean<br>
 *       Formats the log level using its localized name according to the current locale.
 * </ul>
 */
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
